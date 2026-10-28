package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CooperativeCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Application service redeeming cooperative invitation codes.
 * <p>The code and its license are locked while the code is consumed and the sponsored subscription, covering the
 * code's hectares, is created, and all of them are stored in one transaction. A code that does not exist, expired, was
 * already redeemed or was revoked answers the same validation error, and counts as a failed attempt. The attempt is
 * reserved before the code is looked up and given back for any other outcome.</p>
 */
@Service
public class CooperativeCodeRedemptionCommandServiceImpl implements CooperativeCodeRedemptionCommandService {

    private static final String RESOURCE = "cooperative-code-redemption";

    private final InvitationCodeRepository invitationCodeRepository;
    private final CooperativeLicenseRepository cooperativeLicenseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ExternalIamService externalIamService;
    private final RedemptionAttemptLimiter attemptLimiter;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final SubscriptionUnitOfWork work;

    /**
     * Creates the service.
     *
     * @param invitationCodeRepository     the invitation codes
     * @param cooperativeLicenseRepository the cooperative licenses
     * @param subscriptionRepository       the subscriptions
     * @param externalIamService           the IAM role lookup
     * @param attemptLimiter               the limiter of failed attempts
     * @param eventPublisher               the domain event publisher
     * @param clock                        the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeCodeRedemptionCommandServiceImpl(
            InvitationCodeRepository invitationCodeRepository,
            CooperativeLicenseRepository cooperativeLicenseRepository,
            SubscriptionRepository subscriptionRepository,
            ExternalIamService externalIamService,
            RedemptionAttemptLimiter attemptLimiter,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            SubscriptionUnitOfWork work
    ) {
        if (invitationCodeRepository == null) {
            throw new IllegalArgumentException("subscription.invitation_code_repository.null");
        }
        if (cooperativeLicenseRepository == null) {
            throw new IllegalArgumentException("subscription.license_repository.null");
        }
        if (subscriptionRepository == null) {
            throw new IllegalArgumentException("subscription.repository.null");
        }
        if (externalIamService == null) {
            throw new IllegalArgumentException("subscription.iam_service.null");
        }
        if (attemptLimiter == null) {
            throw new IllegalArgumentException("subscription.attempt_limiter.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("subscription.event_publisher.null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("subscription.clock.null");
        }
        if (work == null) {
            throw new IllegalArgumentException("subscription.unit_of_work.null");
        }
        this.work = work;
        this.invitationCodeRepository = invitationCodeRepository;
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.externalIamService = externalIamService;
        this.attemptLimiter = attemptLimiter;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public Result<Subscription, ApplicationError> handle(RedeemCooperativeCodeCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.redemption.command.null"));
        }
        try {
            var producerId = new ProducerId(command.producerId());
            if (!externalIamService.mayRedeemCooperativeCodes(producerId)) {
                return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.redemption.producer_only"));
            }
            var now = clock.instant();
            var retryAfter = attemptLimiter.tryAcquire(producerId, now);
            if (retryAfter.isPresent()) {
                return Result.failure(ApplicationError.tooManyRequests(RESOURCE,
                        "subscription.redemption.too_many_attempts", retryAfter.getAsLong()));
            }
            // In one transaction under the producer's lock, as purchases do: two codes redeemed at once see each
            // other's subscription, so only the first one creates it. The instant is read once the lock is held, so
            // it is never earlier than the start of a subscription committed while this request waited.
            var result = work.exclusive(producerId.producerId(), () -> redeem(command, producerId, clock.instant()));
            if (result.isSuccess()) {
                attemptLimiter.reset(producerId);
            } else if (!isNotRedeemable(result)) {
                attemptLimiter.release(producerId, now);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Runs the redemption once the attempt is reserved. Every failure is returned, never thrown, so the caller can
     * tell which attempts count as failed.
     */
    private Result<Subscription, ApplicationError> redeem(RedeemCooperativeCodeCommand command, ProducerId producerId,
                                                          Instant now) {
        try {
            var code = new CooperativeCode(command.code());
            if (subscriptionRepository.existsActiveByProducerId(producerId, now)) {
                return Result.failure(ApplicationError.conflict("subscription", "subscription.producer.already_active"));
            }

            var invitationCode = invitationCodeRepository.findByFingerprintForRedemption(code.fingerprint());
            if (invitationCode.isEmpty()) {
                return notRedeemable();
            }
            var license = cooperativeLicenseRepository.findByIdForRedemption(invitationCode.get().snapshot().licenseId());
            if (license.isEmpty()) {
                return notRedeemable();
            }

            var redeemedCode = license.get().redeem(invitationCode.get(), producerId, now);
            var subscription = Subscription.activateFromCode(redeemedCode, now);
            invitationCodeRepository.save(invitationCode.get());
            cooperativeLicenseRepository.save(license.get());
            var savedSubscription = subscriptionRepository.save(subscription);

            for (var event : subscription.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            subscription.clearDomainEvents();

            return Result.success(savedSubscription);
        } catch (InvitationCodeNotRedeemableException ex) {
            return notRedeemable();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    private static boolean isNotRedeemable(Result<Subscription, ApplicationError> result) {
        return result.failure()
                .map(error -> InvitationCodeNotRedeemableException.MESSAGE_KEY.equals(error.details()))
                .orElse(false);
    }

    private static Result<Subscription, ApplicationError> notRedeemable() {
        return Result.failure(ApplicationError.validationError("code", InvitationCodeNotRedeemableException.MESSAGE_KEY));
    }
}
