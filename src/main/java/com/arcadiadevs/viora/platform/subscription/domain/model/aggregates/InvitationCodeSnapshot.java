package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;

/**
 * Immutable snapshot of an {@link InvitationCode}.
 *
 * @param id          the code identity
 * @param licenseId   the license whose seat the code holds
 * @param batchId     the batch the code was issued in; null until batches exist
 * @param fingerprint the SHA-256 fingerprint of the code
 * @param status      the lifecycle status
 * @param expiresAt   the last instant the code can be redeemed, exclusive
 * @param redeemedBy  the producer who redeemed it; null unless redeemed
 * @param redeemedAt  when it was redeemed; null unless redeemed
 * @param revision    the optimistic locking revision
 */
public record InvitationCodeSnapshot(
        InvitationCodeId id,
        CooperativeLicenseId licenseId,
        String batchId,
        CodeFingerprint fingerprint,
        InvitationCodeStatus status,
        Instant expiresAt,
        ProducerId redeemedBy,
        Instant redeemedAt,
        Long revision
) {
}
