package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;

/**
 * Aggregate root of a cooperative invitation code: one seat of a cooperative license that a producer redeems once.
 * <p>It is its own root because, when it is issued, there is no subscription to hold it. Only its fingerprint is
 * kept. A code moves from {@code AVAILABLE} to {@code REDEEMED}, {@code EXPIRED} or {@code REVOKED}, and never
 * back.</p>
 */
public class InvitationCode extends AbstractDomainAggregateRoot<InvitationCode> {

    private final InvitationCodeId id;
    private final CooperativeLicenseId licenseId;
    private final String batchId;
    private final CodeFingerprint fingerprint;
    private InvitationCodeStatus status;
    private final Instant expiresAt;
    private ProducerId redeemedBy;
    private Instant redeemedAt;
    private final Long revision;

    private InvitationCode(InvitationCodeSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.batchId = snapshot.batchId();
        this.fingerprint = snapshot.fingerprint();
        this.status = snapshot.status();
        this.expiresAt = snapshot.expiresAt();
        this.redeemedBy = snapshot.redeemedBy();
        this.redeemedAt = snapshot.redeemedAt();
        this.revision = snapshot.revision();
    }

    /**
     * Issues a new available code for a license seat.
     *
     * @param licenseId   the license whose seat the code holds
     * @param fingerprint the fingerprint of the code
     * @param expiresAt   the last instant the code can be redeemed, exclusive
     * @return the available code
     * @throws IllegalArgumentException if any argument is null
     */
    public static InvitationCode issue(CooperativeLicenseId licenseId, CodeFingerprint fingerprint, Instant expiresAt) {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        if (fingerprint == null) {
            throw new IllegalArgumentException("subscription.invitation_code.fingerprint.invalid");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("subscription.invitation_code.expires_at.null");
        }
        return new InvitationCode(new InvitationCodeSnapshot(new InvitationCodeId(), licenseId, null, fingerprint,
                InvitationCodeStatus.AVAILABLE, expiresAt, null, null, 0L));
    }

    /**
     * Rebuilds a code from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the code
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static InvitationCode reconstitute(InvitationCodeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.invitation_code.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.fingerprint() == null
                || snapshot.status() == null || snapshot.expiresAt() == null) {
            throw new IllegalArgumentException("subscription.invitation_code.snapshot.incomplete");
        }
        return new InvitationCode(snapshot);
    }

    /**
     * Checks whether the code can still be redeemed.
     *
     * @param at the instant of the redemption
     * @return true if the code is available and has not expired at {@code at}
     */
    public boolean isRedeemable(Instant at) {
        return status == InvitationCodeStatus.AVAILABLE && at != null && at.isBefore(expiresAt);
    }

    /**
     * Marks the code as redeemed by a producer. Meant to be called through {@code CooperativeLicense.redeem}.
     *
     * @param producerId the producer redeeming the code
     * @param at         the instant of the redemption
     * @throws IllegalArgumentException             if an argument is null
     * @throws InvitationCodeNotRedeemableException if the code is not available or has expired
     */
    void redeem(ProducerId producerId, Instant at) {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (!isRedeemable(at)) {
            throw new InvitationCodeNotRedeemableException();
        }
        this.status = InvitationCodeStatus.REDEEMED;
        this.redeemedBy = producerId;
        this.redeemedAt = at;
    }

    /**
     * Returns the current state of the code.
     *
     * @return the snapshot
     */
    public InvitationCodeSnapshot snapshot() {
        return new InvitationCodeSnapshot(id, licenseId, batchId, fingerprint, status, expiresAt, redeemedBy,
                redeemedAt, revision);
    }
}
