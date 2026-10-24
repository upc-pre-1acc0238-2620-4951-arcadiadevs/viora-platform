package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.QuotaExtensionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.ExtensionCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemExtensionCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RequestQuotaExtensionCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.PlanExtensionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Application service growing plans in force.
 * <p>A paid extension is requested under the producer's lock with an idempotency key, like a purchase, and adds its
 * hectares only when the payment is approved. An extension code is redeemed in one transaction while its license is
 * locked, so concurrent redemptions commit the license hectares one after the other; any reason a code cannot be
 * used answers the same validation error and counts as a failed attempt.</p>
 */
@Service
public class QuotaExtensionCommandServiceImpl implements QuotaExtensionCommandService {

    private static final String OPERATION = "REQUEST_EXTENSION";
    private static final String RESOURCE = "quota-extension-redemption";

    private final SubscriptionRepository subscriptions;
    private final QuotaExtensionRepository extensions;
    private final QuotaExtensionCodeRepository codes;
    private final CooperativeLicenseRepository licenses;
    private final SubscriptionCatalog catalog;
    private final SubscriptionUnitOfWork work;
    private final ExternalIamService iam;
    private final RedemptionAttemptLimiter attemptLimiter;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param subscriptions  the plans
     * @param extensions     the quota extensions
     * @param codes          the extension codes
     * @param licenses       the cooperative licenses
     * @param catalog        the server-owned tariff
     * @param work           transactional producer serialization and idempotency
     * @param iam            the IAM role lookup
     * @param attemptLimiter the limiter of failed code attempts
     * @param clock          the application clock
     */
    public QuotaExtensionCommandServiceImpl(SubscriptionRepository subscriptions, QuotaExtensionRepository extensions,
                                            QuotaExtensionCodeRepository codes, CooperativeLicenseRepository licenses,
                                            SubscriptionCatalog catalog, SubscriptionUnitOfWork work,
                                            ExternalIamService iam, RedemptionAttemptLimiter attemptLimiter,
                                            Clock clock) {
        this.subscriptions = subscriptions;
        this.extensions = extensions;
        this.codes = codes;
        this.licenses = licenses;
        this.catalog = catalog;
        this.work = work;
        this.iam = iam;
        this.attemptLimiter = attemptLimiter;
        this.clock = clock;
    }

    @Override
    public Result<RequestedExtension, ApplicationError> handle(RequestQuotaExtensionCommand command) {
        if (command == null || command.operationId().isBlank() || command.operationId().length() > 128) {
            return Result.failure(ApplicationError.validationError("operationId", "subscription.operation.invalid"));
        }
        final ProducerId owner;
        final IndividualHectaresQuota added;
        try {
            owner = new ProducerId(command.producerId());
            added = new IndividualHectaresQuota(command.addedHa());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.businessRuleViolation("subscription", exception.getMessage()));
        }
        var planId = new SubscriptionId(command.subscriptionId().toString());
        var fingerprint = planId.subscriptionId() + ":" + added.value().toPlainString();
        return work.exclusive(owner.producerId(), () -> {
            var previous = work.findOperation(owner.producerId(), command.operationId());
            if (previous.isPresent()) {
                var operation = previous.get();
                if (!OPERATION.equals(operation.kind()) || !fingerprint.equals(operation.fingerprint())) {
                    return conflict("subscription.operation.conflict");
                }
                return extensions.findById(new QuotaExtensionId(operation.resourceId().toString()))
                        .<Result<RequestedExtension, ApplicationError>>map(found ->
                                Result.success(new RequestedExtension(found, true)))
                        .orElseGet(() -> conflict("subscription.operation.conflict"));
            }
            var plan = subscriptions.findByIdAndProducerId(planId, owner);
            if (plan.isEmpty()) {
                return Result.failure(ApplicationError.notFound("subscription", planId.subscriptionId()));
            }
            var now = clock.instant();
            var current = plan.get().snapshot();
            if (current.mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL) {
                return conflict("subscription.extension.sponsored_plan");
            }
            if (!plan.get().hasActiveEntitlement(now)) {
                return conflict("subscription.extension.not_active");
            }
            if (PlanExtensionPolicy.isInRenewalWindow(current.period(), now)) {
                return conflict("subscription.extension.renewal_window");
            }
            var months = PlanExtensionPolicy.billableMonths(current.period(), now);
            var price = catalog.quoteExtension(current.planCode(), added, months);
            var extension = extensions.save(QuotaExtension.requestPaid(plan.get(), added, months, price, now));
            work.recordOperation(owner.producerId(), command.operationId(), OPERATION, fingerprint,
                    java.util.UUID.fromString(extension.snapshot().id().quotaExtensionId()));
            return Result.success(new RequestedExtension(extension, false));
        });
    }

    @Override
    @Transactional
    public Result<QuotaExtension, ApplicationError> handle(RedeemExtensionCodeCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.redemption.command.null"));
        }
        try {
            var producerId = new ProducerId(command.producerId());
            if (!iam.mayRedeemCooperativeCodes(producerId)) {
                return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.redemption.producer_only"));
            }
            var now = clock.instant();
            var retryAfter = attemptLimiter.tryAcquire(producerId, now);
            if (retryAfter.isPresent()) {
                return Result.failure(ApplicationError.tooManyRequests(RESOURCE,
                        "subscription.redemption.too_many_attempts", retryAfter.getAsLong()));
            }
            var result = redeem(command, producerId, now);
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
    private Result<QuotaExtension, ApplicationError> redeem(RedeemExtensionCodeCommand command, ProducerId producerId,
                                                            Instant now) {
        try {
            var typed = new ExtensionCode(command.code());
            var plan = subscriptions.findCurrent(producerId, now).filter(found -> found.hasActiveEntitlement(now));
            var code = codes.findByFingerprint(typed.fingerprint());
            if (plan.isEmpty() || code.isEmpty()) {
                return notRedeemable();
            }
            var license = licenses.findByIdForRedemption(code.get().snapshot().licenseId());
            // Checked while the license is locked, so two redemptions of the same producer cannot both pass.
            if (license.isEmpty() || extensions.existsByExtensionCodeIdAndProducerId(code.get().snapshot().id(), producerId)) {
                return notRedeemable();
            }
            var extension = license.get().redeemExtensionCode(code.get(), plan.get(), now);
            licenses.save(license.get());
            subscriptions.save(plan.get());
            return Result.success(extensions.save(extension));
        } catch (ExtensionCodeNotRedeemableException ex) {
            return notRedeemable();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    private static boolean isNotRedeemable(Result<QuotaExtension, ApplicationError> result) {
        return result.failure()
                .map(error -> ExtensionCodeNotRedeemableException.MESSAGE_KEY.equals(error.details()))
                .orElse(false);
    }

    private static Result<QuotaExtension, ApplicationError> notRedeemable() {
        return Result.failure(ApplicationError.validationError("code", ExtensionCodeNotRedeemableException.MESSAGE_KEY));
    }

    private static <T> Result<T, ApplicationError> conflict(String key) {
        return Result.failure(ApplicationError.conflict("subscription", key, Map.of("reason", key)));
    }
}
