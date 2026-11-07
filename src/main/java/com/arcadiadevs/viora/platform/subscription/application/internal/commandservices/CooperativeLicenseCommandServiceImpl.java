package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeLicenseCommandService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.MemberRelease;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotReleasableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.ReleaseCooperativeMemberCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RenewCooperativeLicenseCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Application service releasing members from a license and renewing licenses.
 * <p>Releasing a member, which support does with the manager's session since the app shows no button for it, ends
 * the manager's reading at once. The member keeps the plan until the grace is over or the license ends, whichever
 * comes first, and only then is the seat freed. Renewing a license publishes an event so its members' plans follow.</p>
 */
@Service
public class CooperativeLicenseCommandServiceImpl implements CooperativeLicenseCommandService {

    private static final String MEMBER = "cooperative-member";
    private static final String ALREADY_RELEASED = InvitationCodeNotReleasableException.MESSAGE_KEY;

    private final CooperativeLicenseRepository cooperativeLicenseRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DataSharingConsentRepository dataSharingConsentRepository;
    private final SubscriptionUnitOfWork work;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration grace;

    /**
     * Creates the service.
     *
     * @param cooperativeLicenseRepository the licenses
     * @param invitationCodeRepository     the codes members redeemed
     * @param subscriptionRepository       the sponsored plans
     * @param dataSharingConsentRepository the consents to share data
     * @param work                         the per producer lock and transaction
     * @param eventPublisher               the domain event publisher
     * @param clock                        the application clock
     * @param graceDays                    the days a released member keeps the plan
     * @throws IllegalArgumentException if any dependency is null or the grace is negative
     */
    public CooperativeLicenseCommandServiceImpl(CooperativeLicenseRepository cooperativeLicenseRepository,
                                                InvitationCodeRepository invitationCodeRepository,
                                                SubscriptionRepository subscriptionRepository,
                                                DataSharingConsentRepository dataSharingConsentRepository,
                                                SubscriptionUnitOfWork work, ApplicationEventPublisher eventPublisher,
                                                Clock clock,
                                                @Value("${viora.subscription.member-release.grace-days:30}")
                                                long graceDays) {
        if (cooperativeLicenseRepository == null || invitationCodeRepository == null
                || subscriptionRepository == null || dataSharingConsentRepository == null || work == null
                || eventPublisher == null || clock == null || graceDays < 0) {
            throw new IllegalArgumentException("subscription.license_service.dependency.null");
        }
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
        this.invitationCodeRepository = invitationCodeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dataSharingConsentRepository = dataSharingConsentRepository;
        this.work = work;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.grace = Duration.ofDays(graceDays);
    }

    @Override
    public Result<MemberRelease, ApplicationError> handle(ReleaseCooperativeMemberCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.release.command.null"));
        }
        try {
            var managerId = new ManagerId(command.managerId());
            var licenseId = new CooperativeLicenseId(command.licenseId());
            var memberId = new ProducerId(command.memberId());
            return work.exclusive(memberId.producerId(), () -> release(managerId, licenseId, memberId));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<CooperativeLicense, ApplicationError> handle(RenewCooperativeLicenseCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.renewal.command.null"));
        }
        try {
            var licenseId = new CooperativeLicenseId(command.licenseId());
            var license = cooperativeLicenseRepository.findByIdForRedemption(licenseId);
            if (license.isEmpty()) {
                return Result.failure(ApplicationError.notFound("cooperative-license", licenseId.cooperativeLicenseId()));
            }
            license.get().renew(command.newEndsAt(), clock.instant());
            var saved = cooperativeLicenseRepository.save(license.get());
            license.get().domainEvents().forEach(eventPublisher::publishEvent);
            license.get().clearDomainEvents();
            return Result.success(saved);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("newEndsAt", ex.getMessage()));
        }
    }

    /**
     * The release itself, under the member's lock and in one transaction: the license, the member's code, plan and
     * consent change together.
     */
    private Result<MemberRelease, ApplicationError> release(ManagerId managerId, CooperativeLicenseId licenseId,
                                                            ProducerId memberId) {
        var license = cooperativeLicenseRepository.findByIdForRedemption(licenseId)
                .filter(found -> managerId.equals(found.snapshot().managerId()));
        // A license of another manager answers the same as one that does not exist.
        if (license.isEmpty()) {
            return Result.failure(ApplicationError.notFound("cooperative-license", licenseId.cooperativeLicenseId()));
        }
        var now = clock.instant();
        var plan = subscriptionRepository.findCurrent(memberId, now)
                .filter(found -> found.hasActiveEntitlement(now))
                .filter(found -> licenseId.equals(found.snapshot().licenseId()));
        var code = invitationCodeRepository.findRedeemedByProducerId(memberId).stream()
                .filter(found -> licenseId.equals(found.snapshot().licenseId()))
                .findFirst();
        if (plan.isEmpty() || code.isEmpty()) {
            return Result.failure(ApplicationError.notFound(MEMBER, memberId.producerId()));
        }
        var planEnd = plan.get().snapshot().period().endsAt();
        Instant seatFreesAt = now.plus(grace).isBefore(planEnd) ? now.plus(grace) : planEnd;
        try {
            license.get().holdMemberSeat(code.get(), seatFreesAt, now);
        } catch (InvitationCodeNotReleasableException ex) {
            return Result.failure(ApplicationError.conflict(MEMBER, ALREADY_RELEASED,
                    Map.of("reason", ALREADY_RELEASED)));
        }
        plan.get().endEarly(seatFreesAt);
        invitationCodeRepository.save(code.get());
        subscriptionRepository.save(plan.get());
        var planId = plan.get().snapshot().id();
        for (var consent : dataSharingConsentRepository.findAllByProducerId(memberId)) {
            if (consent.snapshot().subscriptionId().equals(planId) && consent.endWithRelease(now)) {
                dataSharingConsentRepository.save(consent);
            }
        }
        return Result.success(new MemberRelease(memberId.producerId(), now, seatFreesAt, seatFreesAt));
    }
}
