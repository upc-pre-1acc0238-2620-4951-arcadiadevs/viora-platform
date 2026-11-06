package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.ManagerCodeRedemptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.ManagerCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemManagerCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Application service redeeming manager activation codes.
 * <p>Under the manager's lock, the code and its license are locked while the code is consumed and the manager put in
 * charge, all in one transaction. A manager already in charge of a license in force cannot take another one. A code
 * that does not exist, expired, was already redeemed or was revoked, or whose license is not in force, answers the same
 * validation error, whose {@code reason} the app reads, and counts as a failed attempt. The attempt is reserved before
 * the code is looked up and given back for any other outcome.</p>
 */
@Service
public class ManagerCodeRedemptionCommandServiceImpl implements ManagerCodeRedemptionCommandService {

    private static final String RESOURCE = "manager-code-redemption";
    private static final String ALREADY_LICENSED = "subscription.manager.already_licensed";

    private final ManagerActivationCodeRepository codes;
    private final CooperativeLicenseRepository licenses;
    private final ExternalIamService externalIamService;
    private final RedemptionAttemptLimiter attemptLimiter;
    private final Clock clock;
    private final SubscriptionUnitOfWork work;

    /**
     * Creates the service.
     *
     * @param codes              the manager activation codes
     * @param licenses           the cooperative licenses
     * @param externalIamService the IAM role lookup
     * @param attemptLimiter     the limiter of failed attempts
     * @param clock              the application clock
     * @param work               the per user lock and transaction
     * @throws IllegalArgumentException if any dependency is null
     */
    public ManagerCodeRedemptionCommandServiceImpl(
            ManagerActivationCodeRepository codes,
            CooperativeLicenseRepository licenses,
            ExternalIamService externalIamService,
            RedemptionAttemptLimiter attemptLimiter,
            Clock clock,
            SubscriptionUnitOfWork work
    ) {
        if (codes == null) {
            throw new IllegalArgumentException("subscription.manager_code_repository.null");
        }
        if (licenses == null) {
            throw new IllegalArgumentException("subscription.license_repository.null");
        }
        if (externalIamService == null) {
            throw new IllegalArgumentException("subscription.iam_service.null");
        }
        if (attemptLimiter == null) {
            throw new IllegalArgumentException("subscription.attempt_limiter.null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("subscription.clock.null");
        }
        if (work == null) {
            throw new IllegalArgumentException("subscription.unit_of_work.null");
        }
        this.codes = codes;
        this.licenses = licenses;
        this.externalIamService = externalIamService;
        this.attemptLimiter = attemptLimiter;
        this.clock = clock;
        this.work = work;
    }

    @Override
    public Result<CooperativeLicense, ApplicationError> handle(RedeemManagerCodeCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.redemption.command.null"));
        }
        try {
            var managerId = new ManagerId(command.managerId());
            if (!externalIamService.isTechnicalManager(managerId)) {
                return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.manager_redemption.manager_only"));
            }
            var now = clock.instant();
            var retryAfter = attemptLimiter.tryAcquire(managerId, now);
            if (retryAfter.isPresent()) {
                return Result.failure(ApplicationError.tooManyRequests(RESOURCE,
                        "subscription.redemption.too_many_attempts", retryAfter.getAsLong()));
            }
            var result = work.exclusive(managerId.managerId(), () -> redeem(command, managerId, clock.instant()));
            if (result.isSuccess()) {
                attemptLimiter.reset(managerId);
            } else if (!isNotRedeemable(result)) {
                attemptLimiter.release(managerId, now);
            }
            return result;
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Runs the redemption once the attempt is reserved, under the manager's lock and in one transaction. Every failure
     * is returned, never thrown, so {@link #handle} can tell which attempts count as failed.
     */
    private Result<CooperativeLicense, ApplicationError> redeem(RedeemManagerCodeCommand command, ManagerId managerId,
                                                                Instant now) {
        try {
            var typed = new ManagerCode(command.code());
            var held = licenses.findByManagerIdForUpdate(managerId);
            if (held.isPresent() && held.get().isInForce(now)) {
                return Result.failure(ApplicationError.conflict("license", ALREADY_LICENSED,
                        Map.of("reason", ALREADY_LICENSED)));
            }

            var code = codes.findByFingerprintForRedemption(typed.fingerprint());
            if (code.isEmpty()) {
                return notRedeemable();
            }
            var license = licenses.findByIdForRedemption(code.get().snapshot().licenseId());
            if (license.isEmpty()) {
                return notRedeemable();
            }

            license.get().assignManager(code.get(), managerId, now);
            // One license per manager: the one that ran out stops pointing at them.
            if (held.isPresent() && !held.get().snapshot().id().equals(license.get().snapshot().id())) {
                held.get().releaseManager(managerId);
                licenses.save(held.get());
            }
            codes.save(code.get());
            return Result.success(licenses.save(license.get()));
        } catch (ManagerCodeNotRedeemableException ex) {
            return notRedeemable();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    private static boolean isNotRedeemable(Result<CooperativeLicense, ApplicationError> result) {
        return result.failure()
                .map(error -> ManagerCodeNotRedeemableException.MESSAGE_KEY.equals(error.details()))
                .orElse(false);
    }

    private static Result<CooperativeLicense, ApplicationError> notRedeemable() {
        return Result.failure(ApplicationError.validationError("code", ManagerCodeNotRedeemableException.MESSAGE_KEY,
                Map.of("reason", ManagerCodeNotRedeemableException.MESSAGE_KEY)));
    }
}
