package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.InvitationCodeBatch;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.InvitationCodeCommandService;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.IssuedInvitationCode;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeCipher;
import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeGenerator;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseAreaException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseSeatsException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRevocableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.IssueInvitationCodesCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RevokeInvitationCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Application service of the invitation codes a technical manager issues and revokes.
 * <p>Everything happens under the manager's lock, with their license locked: the codes whose time is over are first
 * marked as expired, so their seats and hectares are free again, and then the batch is checked against what the
 * license has left. Every code is unique among all the licenses: a drawn code whose fingerprint was ever issued is
 * drawn again, and a batch that loses a race for a code against another license is issued again from scratch.</p>
 */
@Service
public class InvitationCodeCommandServiceImpl implements InvitationCodeCommandService {

    /** Most codes in one batch. */
    public static final int MAX_BATCH = 50;
    /** The validities a manager can choose, in days. */
    public static final Set<Integer> VALIDITY_DAYS = Set.of(7, 15, 30);

    private static final String RESOURCE = "invitation-codes";
    private static final int DRAWS_PER_CODE = 20;
    private static final int BATCH_ATTEMPTS = 3;
    private static final String NO_LICENSE = "subscription.manager.no_license_in_force";
    private static final String DRAWS_EXHAUSTED = "subscription.invitation_code.generation.exhausted";

    private final InvitationCodeRepository codes;
    private final CooperativeLicenseRepository licenses;
    private final ExternalIamService externalIamService;
    private final InvitationCodeGenerator generator;
    private final InvitationCodeCipher cipher;
    private final Clock clock;
    private final SubscriptionUnitOfWork work;

    /**
     * Creates the service.
     *
     * @param codes              the invitation codes
     * @param licenses           the cooperative licenses
     * @param externalIamService the IAM role lookup
     * @param generator          the random source of new codes
     * @param cipher             the encryption of codes still to redeem
     * @param clock              the application clock
     * @param work               the per user lock and transaction
     * @throws IllegalArgumentException if any dependency is null
     */
    public InvitationCodeCommandServiceImpl(
            InvitationCodeRepository codes,
            CooperativeLicenseRepository licenses,
            ExternalIamService externalIamService,
            InvitationCodeGenerator generator,
            InvitationCodeCipher cipher,
            Clock clock,
            SubscriptionUnitOfWork work
    ) {
        if (codes == null || licenses == null || externalIamService == null || generator == null || cipher == null
                || clock == null || work == null) {
            throw new IllegalArgumentException("subscription.invitation_code_service.dependency.null");
        }
        this.codes = codes;
        this.licenses = licenses;
        this.externalIamService = externalIamService;
        this.generator = generator;
        this.cipher = cipher;
        this.clock = clock;
        this.work = work;
    }

    @Override
    public Result<InvitationCodeBatch, ApplicationError> handle(IssueInvitationCodesCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.invitation_code.command.null"));
        }
        try {
            var managerId = new ManagerId(command.managerId());
            if (!externalIamService.isTechnicalManager(managerId)) {
                return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.invitation_codes.manager_only"));
            }
            if (command.quantity() < 1 || command.quantity() > MAX_BATCH) {
                return Result.failure(ApplicationError.validationError("quantity",
                        "subscription.invitation_code.batch.quantity"));
            }
            if (!VALIDITY_DAYS.contains(command.validityDays())) {
                return Result.failure(ApplicationError.validationError("validityDays",
                        "subscription.invitation_code.batch.validity"));
            }
            var quota = new HectaresQuota(command.hectaresPerCode());
            if (!cipher.isAvailable()) {
                return Result.failure(ApplicationError.unavailable(RESOURCE,
                        "subscription.invitation_code.cipher.unavailable"));
            }
            for (var attempt = 1; ; attempt++) {
                try {
                    return work.exclusive(managerId.managerId(), () -> issue(command, managerId, quota));
                } catch (DataIntegrityViolationException ex) {
                    // Another license took one of the drawn codes at the same moment: the whole batch is drawn again.
                    if (attempt == BATCH_ATTEMPTS) {
                        return Result.failure(ApplicationError.unavailable(RESOURCE, DRAWS_EXHAUSTED));
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<InvitationCode, ApplicationError> handle(RevokeInvitationCodeCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.invitation_code.command.null"));
        }
        try {
            var managerId = new ManagerId(command.managerId());
            if (!externalIamService.isTechnicalManager(managerId)) {
                return Result.failure(ApplicationError.forbidden(RESOURCE, "subscription.invitation_codes.manager_only"));
            }
            var codeId = new InvitationCodeId(command.invitationCodeId());
            return work.exclusive(managerId.managerId(), () -> revoke(codeId, managerId));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Issues the batch once the manager's lock is held. Every failure is returned, never thrown, except a code
     * another license stored at the same moment, which the caller draws again.
     */
    private Result<InvitationCodeBatch, ApplicationError> issue(IssueInvitationCodesCommand command, ManagerId managerId,
                                                       HectaresQuota quota) {
        var now = clock.instant();
        var held = licenses.findByManagerIdForUpdate(managerId);
        if (held.isEmpty() || !held.get().isInForce(now)) {
            return Result.failure(ApplicationError.forbidden("license", NO_LICENSE));
        }
        var license = held.get();
        var licenseId = license.snapshot().id();
        expireOverdue(license, now);
        var seatsUsed = codes.countSeatsInUse(licenseId, now);

        var drawn = draw(command.quantity());
        if (drawn.isEmpty()) {
            return Result.failure(ApplicationError.unavailable(RESOURCE, DRAWS_EXHAUSTED));
        }
        var values = drawn.get();
        var sealed = values.stream()
                .map(value -> new InvitationCode.IssuedCode(new CooperativeCode(value).fingerprint(),
                        value.substring(value.length() - 2), cipher.encrypt(value)))
                .toList();
        var chosenEnd = now.plus(Duration.ofDays(command.validityDays()));
        var licenseEnd = license.snapshot().period().endsAt();
        var expiresAt = chosenEnd.isAfter(licenseEnd) ? licenseEnd : chosenEnd;

        final List<InvitationCode> issued;
        try {
            issued = license.issueCodes(sealed, quota, seatsUsed, now, expiresAt);
        } catch (InsufficientLicenseSeatsException ex) {
            var capacity = license.capacity(seatsUsed, BigDecimal.ZERO);
            return Result.failure(ApplicationError.conflict("license", InsufficientLicenseSeatsException.MESSAGE_KEY,
                    Map.of("reason", InsufficientLicenseSeatsException.MESSAGE_KEY,
                            "seatsFree", capacity.seatsFree())));
        } catch (InsufficientLicenseAreaException ex) {
            var capacity = license.capacity(seatsUsed, BigDecimal.ZERO);
            return Result.failure(ApplicationError.conflict("license", InsufficientLicenseAreaException.MESSAGE_KEY,
                    Map.of("reason", InsufficientLicenseAreaException.MESSAGE_KEY,
                            "freeAreaHa", capacity.freeAreaHa())));
        }
        issued.forEach(codes::save);
        licenses.save(license);

        var batch = new ArrayList<IssuedInvitationCode>();
        for (var index = 0; index < issued.size(); index++) {
            batch.add(new IssuedInvitationCode(issued.get(index).snapshot().id().invitationCodeId(), values.get(index)));
        }
        var first = issued.getFirst().snapshot();
        return Result.success(new InvitationCodeBatch(first.batchId(), now, expiresAt, quota.hectares(), batch));
    }

    /**
     * Revokes the code once the manager's lock is held. Every failure is returned, never thrown.
     */
    private Result<InvitationCode, ApplicationError> revoke(InvitationCodeId codeId, ManagerId managerId) {
        var now = clock.instant();
        var held = licenses.findByManagerIdForUpdate(managerId);
        var found = codes.findByIdForUpdate(codeId);
        // A code of another license answers the same as one that does not exist.
        if (held.isEmpty() || found.isEmpty()
                || !found.get().snapshot().licenseId().equals(held.get().snapshot().id())) {
            return Result.failure(ApplicationError.notFound("invitation_code", codeId.invitationCodeId()));
        }
        var license = held.get();
        var code = found.get();
        try {
            license.revokeCode(code, now);
        } catch (InvitationCodeNotRevocableException ex) {
            // A code whose time is over is marked as expired now, so its seat and hectares are free again.
            if (license.expireCode(code, now)) {
                codes.save(code);
                licenses.save(license);
            }
            var properties = new HashMap<String, Object>();
            properties.put("reason", ex.getMessage());
            if (ex.redeemedAt() != null) {
                properties.put("redeemedAt", ex.redeemedAt().toString());
            }
            return Result.failure(ApplicationError.conflict("invitation_code", ex.getMessage(), properties));
        }
        codes.save(code);
        licenses.save(license);
        return Result.success(code);
    }

    private void expireOverdue(CooperativeLicense license, Instant now) {
        for (var code : codes.findOverdueByLicenseId(license.snapshot().id(), now)) {
            if (license.expireCode(code, now)) {
                codes.save(code);
            }
        }
        // A released member's seat goes back once their grace is over.
        for (var code : codes.findSeatOverdueByLicenseId(license.snapshot().id(), now)) {
            if (license.releaseOverdueSeat(code, now)) {
                codes.save(code);
            }
        }
    }

    /**
     * Draws {@code quantity} codes never issued before, nor repeated in the batch.
     *
     * @return the codes, or empty if too many draws hit codes already issued
     */
    private Optional<List<String>> draw(int quantity) {
        var values = new ArrayList<String>();
        var seen = new HashSet<CodeFingerprint>();
        while (values.size() < quantity) {
            String accepted = null;
            for (var draw = 0; draw < DRAWS_PER_CODE && accepted == null; draw++) {
                var candidate = generator.next();
                var fingerprint = new CooperativeCode(candidate).fingerprint();
                if (!seen.contains(fingerprint) && !codes.existsByFingerprint(fingerprint)) {
                    seen.add(fingerprint);
                    accepted = candidate;
                }
            }
            if (accepted == null) {
                return Optional.empty();
            }
            values.add(accepted);
        }
        return Optional.of(values);
    }
}
