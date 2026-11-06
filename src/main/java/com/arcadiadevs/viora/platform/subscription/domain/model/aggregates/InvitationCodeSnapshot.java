package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
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
 * @param quota       the hectares the code covers, committed on the license when it was issued
 * @param status      the lifecycle status
 * @param expiresAt   the last instant the code can be redeemed, exclusive
 * @param redeemedBy  the producer who redeemed it; null unless redeemed
 * @param redeemedAt  when it was redeemed; null unless redeemed
 * @param revision    the optimistic locking revision
 * @param hint        the last two characters of the code, kept to show it masked; null for codes issued before
 * @param sealedCode  the code encrypted, kept only while it can be redeemed so the manager can share it again
 * @param issuedAt    when the code was issued; null for codes issued before it was kept
 */
public record InvitationCodeSnapshot(
        InvitationCodeId id,
        CooperativeLicenseId licenseId,
        String batchId,
        CodeFingerprint fingerprint,
        HectaresQuota quota,
        InvitationCodeStatus status,
        Instant expiresAt,
        ProducerId redeemedBy,
        Instant redeemedAt,
        Long revision,
        String hint,
        String sealedCode,
        Instant issuedAt
) {

    /**
     * Snapshot of a code issued before codes kept their hint, sealed value and issue instant, such as a seeded one.
     *
     * @param id          the code identity
     * @param licenseId   the license whose seat the code holds
     * @param batchId     the batch it was issued in, if any
     * @param fingerprint the SHA-256 fingerprint of the code
     * @param quota       the hectares the code covers
     * @param status      the lifecycle status
     * @param expiresAt   the last instant the code can be redeemed, exclusive
     * @param redeemedBy  the producer who redeemed it, if any
     * @param redeemedAt  when it was redeemed, if it was
     * @param revision    the optimistic locking revision
     */
    public InvitationCodeSnapshot(InvitationCodeId id, CooperativeLicenseId licenseId, String batchId,
                                  CodeFingerprint fingerprint, HectaresQuota quota, InvitationCodeStatus status,
                                  Instant expiresAt, ProducerId redeemedBy, Instant redeemedAt, Long revision) {
        this(id, licenseId, batchId, fingerprint, quota, status, expiresAt, redeemedBy, redeemedAt, revision, null,
                null, null);
    }
}
