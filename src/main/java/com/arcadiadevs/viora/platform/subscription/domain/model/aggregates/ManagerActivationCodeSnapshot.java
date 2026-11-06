package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

import java.time.Instant;

/**
 * Immutable snapshot of a {@link ManagerActivationCode}.
 *
 * @param id          the code identity
 * @param licenseId   the license the code puts in the hands of a manager
 * @param fingerprint the SHA-256 fingerprint of the code
 * @param status      the lifecycle status
 * @param expiresAt   the last instant the code can be redeemed, exclusive
 * @param redeemedBy  the manager who redeemed it; null until it is redeemed
 * @param redeemedAt  when it was redeemed; null until it is redeemed
 * @param revision    the optimistic locking revision
 */
public record ManagerActivationCodeSnapshot(
        ManagerCodeId id,
        CooperativeLicenseId licenseId,
        CodeFingerprint fingerprint,
        ManagerCodeStatus status,
        Instant expiresAt,
        ManagerId redeemedBy,
        Instant redeemedAt,
        Long revision
) {
}
