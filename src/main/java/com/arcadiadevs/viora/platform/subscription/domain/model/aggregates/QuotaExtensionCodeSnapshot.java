package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;

import java.time.Instant;

/**
 * Immutable snapshot of a {@link QuotaExtensionCode}.
 *
 * @param id          the code identity
 * @param licenseId   the license the code draws hectares from
 * @param fingerprint the SHA-256 fingerprint of the code
 * @param addedArea   the hectares each producer gets when redeeming it
 * @param status      the lifecycle status
 * @param expiresAt   the last instant the code can be redeemed, exclusive
 * @param revision    the optimistic locking revision
 */
public record QuotaExtensionCodeSnapshot(
        ExtensionCodeId id,
        CooperativeLicenseId licenseId,
        CodeFingerprint fingerprint,
        HectaresQuota addedArea,
        ExtensionCodeStatus status,
        Instant expiresAt,
        Long revision
) {
}
