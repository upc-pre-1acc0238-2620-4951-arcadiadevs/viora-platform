package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeCodePreview;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeCodeQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeCodePreviewQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Shows a producer what a cooperative code would give them before they redeem it: the organization, the manager who
 * would read their data, the hectares and until when, and whether they already registered more hectares.
 * <p>Nothing changes: the code stays available. Since it tells whether a code exists, it takes from the same failed
 * attempts as the redemption, so it cannot be used to guess codes.</p>
 */
@Service
@Transactional(readOnly = true)
public class CooperativeCodeQueryServiceImpl implements CooperativeCodeQueryService {

    private static final String RESOURCE = "cooperative-code-preview";
    private static final String ALREADY_ACTIVE = "subscription.producer.already_active";

    private final InvitationCodeRepository invitationCodeRepository;
    private final CooperativeLicenseRepository cooperativeLicenseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ExternalIamService externalIamService;
    private final ExternalProfileService externalProfileService;
    private final ExternalOrchardService externalOrchardService;
    private final RedemptionAttemptLimiter attemptLimiter;
    private final Clock clock;
    private final String consentTextVersion;

    /**
     * Creates the query service.
     *
     * @param invitationCodeRepository     the invitation codes
     * @param cooperativeLicenseRepository the cooperative licenses
     * @param subscriptionRepository       the plans, to tell a producer who already has one
     * @param externalIamService           the IAM role lookup
     * @param externalProfileService       the names of the managers
     * @param externalOrchardService       the hectares the producer registered
     * @param attemptLimiter               the limiter of failed attempts
     * @param clock                        the application clock
     * @param consentTextVersion           the version of the consent text in force
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeCodeQueryServiceImpl(
            InvitationCodeRepository invitationCodeRepository,
            CooperativeLicenseRepository cooperativeLicenseRepository,
            SubscriptionRepository subscriptionRepository,
            ExternalIamService externalIamService,
            ExternalProfileService externalProfileService,
            ExternalOrchardService externalOrchardService,
            RedemptionAttemptLimiter attemptLimiter,
            Clock clock,
            @Value("${viora.subscription.consent.text-version}") String consentTextVersion
    ) {
        if (invitationCodeRepository == null || cooperativeLicenseRepository == null || subscriptionRepository == null || externalIamService == null
                || externalProfileService == null || externalOrchardService == null || attemptLimiter == null
                || clock == null || consentTextVersion == null) {
            throw new IllegalArgumentException("subscription.code_preview.dependency.null");
        }
        this.invitationCodeRepository = invitationCodeRepository;
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.externalIamService = externalIamService;
        this.externalProfileService = externalProfileService;
        this.externalOrchardService = externalOrchardService;
        this.attemptLimiter = attemptLimiter;
        this.clock = clock;
        this.consentTextVersion = consentTextVersion;
    }

    @Override
    public Result<CooperativeCodePreview, ApplicationError> handle(GetCooperativeCodePreviewQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "subscription.code_preview.query.null"));
        }
        var producerId = query.producerId();
        if (!externalIamService.mayRedeemCooperativeCodes(producerId)) {
            return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.redemption.producer_only"));
        }
        var now = clock.instant();
        var retryAfter = attemptLimiter.tryAcquire(producerId, now);
        if (retryAfter.isPresent()) {
            return Result.failure(ApplicationError.tooManyRequests(RESOURCE,
                    "subscription.redemption.too_many_attempts", retryAfter.getAsLong()));
        }
        var result = preview(query.code(), producerId, now);
        // Only a code that cannot be redeemed counts; looking at a good one is not a failed attempt.
        if (!isNotRedeemable(result)) {
            attemptLimiter.release(producerId, now);
        }
        return result;
    }

    private Result<CooperativeCodePreview, ApplicationError> preview(String typed, ProducerId producerId,
                                                                    Instant now) {
        final CooperativeCode code;
        try {
            code = new CooperativeCode(typed);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("code", ex.getMessage()));
        }
        if (subscriptionRepository.existsActiveByProducerId(producerId, now)) {
            return Result.failure(ApplicationError.conflict("subscription", ALREADY_ACTIVE,
                    Map.of("reason", ALREADY_ACTIVE)));
        }
        var invitation = invitationCodeRepository.findByFingerprint(code.fingerprint()).filter(found -> found.isRedeemable(now));
        if (invitation.isEmpty()) {
            return notRedeemable();
        }
        var license = cooperativeLicenseRepository.findById(invitation.get().snapshot().licenseId()).filter(found -> found.isInForce(now));
        if (license.isEmpty()) {
            return notRedeemable();
        }
        var licenseSnapshot = license.get().snapshot();
        var quota = invitation.get().snapshot().quota().hectares();
        var registered = externalOrchardService.registeredAreaHa(producerId);
        return Result.success(new CooperativeCodePreview(
                licenseSnapshot.cooperativeName(),
                externalProfileService.findFullName(licenseSnapshot.managerId()).orElse(null),
                quota,
                licenseSnapshot.period().endsAt(),
                registered,
                registered.compareTo(quota) > 0,
                consentTextVersion));
    }

    private static boolean isNotRedeemable(Result<CooperativeCodePreview, ApplicationError> result) {
        return result.failure()
                .map(error -> InvitationCodeNotRedeemableException.MESSAGE_KEY.equals(error.details()))
                .orElse(false);
    }

    private static Result<CooperativeCodePreview, ApplicationError> notRedeemable() {
        return Result.failure(ApplicationError.validationError("code", InvitationCodeNotRedeemableException.MESSAGE_KEY,
                Map.of("reason", InvitationCodeNotRedeemableException.MESSAGE_KEY)));
    }
}
