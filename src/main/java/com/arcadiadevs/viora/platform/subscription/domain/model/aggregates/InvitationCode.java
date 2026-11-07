package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotReleasableException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRevocableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;

/**
 * Aggregate root of a cooperative invitation code: one seat of a cooperative license, with the hectares it
 * covers, that a producer redeems once.
 * <p>It is its own root because, when it is issued, there is no subscription to hold it. Its fingerprint is kept for
 * good, so a code is unique among every license and never issued twice. While it can be redeemed it also keeps the
 * code encrypted, so the manager can share it again; once it is redeemed, expires or is revoked, only its last two
 * characters remain. A code moves from {@code AVAILABLE} to {@code REDEEMED}, {@code EXPIRED} or {@code REVOKED},
 * and never back.</p>
 */
public class InvitationCode extends AbstractDomainAggregateRoot<InvitationCode> {

    private final InvitationCodeId id;
    private final CooperativeLicenseId licenseId;
    private final String batchId;
    private final CodeFingerprint fingerprint;
    private final HectaresQuota quota;
    private InvitationCodeStatus status;
    private final Instant expiresAt;
    private ProducerId redeemedBy;
    private Instant redeemedAt;
    private final Long revision;
    private final String hint;
    private String sealedCode;
    private final Instant issuedAt;
    private Instant seatHeldUntil;

    private InvitationCode(InvitationCodeSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.batchId = snapshot.batchId();
        this.fingerprint = snapshot.fingerprint();
        this.quota = snapshot.quota();
        this.status = snapshot.status();
        this.expiresAt = snapshot.expiresAt();
        this.redeemedBy = snapshot.redeemedBy();
        this.redeemedAt = snapshot.redeemedAt();
        this.revision = snapshot.revision();
        this.hint = snapshot.hint();
        this.sealedCode = snapshot.sealedCode();
        this.issuedAt = snapshot.issuedAt();
        this.seatHeldUntil = snapshot.seatHeldUntil();
    }

    /**
     * Issues a new available code for a license seat. Only {@link CooperativeLicense} calls it, so the hectares are
     * always committed on the license.
     *
     * @param licenseId   the license whose seat the code holds
     * @param fingerprint the fingerprint of the code
     * @param quota       the hectares the code covers
     * @param expiresAt   the last instant the code can be redeemed, exclusive
     * @return the available code
     * @throws IllegalArgumentException if any argument is null
     */
    static InvitationCode issue(CooperativeLicenseId licenseId, CodeFingerprint fingerprint, HectaresQuota quota,
                                Instant expiresAt) {
        return issue(licenseId, null, new IssuedCode(fingerprint, null, null), quota, null, expiresAt);
    }

    /**
     * Issues a new available code of a batch. Only {@link CooperativeLicense#issueCodes} calls it.
     *
     * @param licenseId the license whose seat the code holds
     * @param batchId   the batch the code belongs to
     * @param issued    the fingerprint, hint and sealed value of the code
     * @param quota     the hectares the code covers
     * @param issuedAt  when the batch was issued
     * @param expiresAt the last instant the code can be redeemed, exclusive
     * @return the available code
     * @throws IllegalArgumentException if a required argument is null
     */
    static InvitationCode issue(CooperativeLicenseId licenseId, String batchId, IssuedCode issued,
                                HectaresQuota quota, Instant issuedAt, Instant expiresAt) {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        if (issued == null || issued.fingerprint() == null) {
            throw new IllegalArgumentException("subscription.invitation_code.fingerprint.invalid");
        }
        if (quota == null) {
            throw new IllegalArgumentException("subscription.quota.null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("subscription.invitation_code.expires_at.null");
        }
        return new InvitationCode(new InvitationCodeSnapshot(new InvitationCodeId(), licenseId, batchId,
                issued.fingerprint(), quota, InvitationCodeStatus.AVAILABLE, expiresAt, null, null, 0L,
                issued.hint(), issued.sealedCode(), issuedAt));
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
                || snapshot.quota() == null
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
     * Checks whether the code is still available although its time is over, so its seat and hectares are still
     * held until it is marked as expired.
     *
     * @param at the instant to check
     * @return true if the code is available and expired at {@code at}
     */
    public boolean isOverdue(Instant at) {
        return status == InvitationCodeStatus.AVAILABLE && at != null && !at.isBefore(expiresAt);
    }

    /**
     * Tells the status the code has at an instant, counting as expired an available code whose time is over.
     *
     * @param at the instant to check
     * @return the status at {@code at}
     */
    public InvitationCodeStatus statusAt(Instant at) {
        if (isOverdue(at)) {
            return InvitationCodeStatus.EXPIRED;
        }
        return isSeatOverdue(at) ? InvitationCodeStatus.RELEASED : status;
    }

    /**
     * Checks whether the member of a redeemed code was released and their grace is over, so its seat and hectares
     * are still held only until the code is marked as released.
     *
     * @param at the instant to check
     * @return true if the code is redeemed and its seat hold ended at {@code at}
     */
    public boolean isSeatOverdue(Instant at) {
        return status == InvitationCodeStatus.REDEEMED && seatHeldUntil != null && at != null
                && !at.isBefore(seatHeldUntil);
    }

    /**
     * Keeps the seat of a released member until their plan ends. Meant to be called through
     * {@code CooperativeLicense.holdMemberSeat}.
     *
     * @param until when the seat is freed
     * @throws IllegalArgumentException          if {@code until} is null
     * @throws InvitationCodeNotReleasableException if the code is not redeemed or its seat is already on hold
     */
    void holdSeatUntil(Instant until) {
        if (until == null) {
            throw new IllegalArgumentException("subscription.invitation_code.seat_hold.null");
        }
        if (status != InvitationCodeStatus.REDEEMED || seatHeldUntil != null) {
            throw new InvitationCodeNotReleasableException();
        }
        this.seatHeldUntil = until;
    }

    /**
     * Frees the seat of a redeemed code whose member left. Meant to be called through
     * {@code CooperativeLicense.releaseMemberCode}, which gives its hectares back.
     *
     * @param at the instant of the release
     * @return true if the code was redeemed and is now released; false if nothing changed
     */
    boolean release(Instant at) {
        if (status != InvitationCodeStatus.REDEEMED || at == null) {
            return false;
        }
        this.status = InvitationCodeStatus.RELEASED;
        this.seatHeldUntil = null;
        return true;
    }

    /**
     * Marks the code as redeemed by a producer. Meant to be called through {@code CooperativeLicense.redeem}. The
     * code itself is no longer kept: only its last characters remain.
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
        this.sealedCode = null;
    }

    /**
     * Withdraws a code nobody redeemed yet. Meant to be called through {@code CooperativeLicense.revokeCode}, which
     * gives its seat and hectares back.
     *
     * @param at the instant of the revocation
     * @throws InvitationCodeNotRevocableException if the code was redeemed, expired or was already revoked
     */
    void revoke(Instant at) {
        if (status == InvitationCodeStatus.REDEEMED) {
            throw InvitationCodeNotRevocableException.alreadyRedeemed(redeemedAt);
        }
        if (!isRedeemable(at)) {
            throw InvitationCodeNotRevocableException.notAvailable();
        }
        this.status = InvitationCodeStatus.REVOKED;
        this.sealedCode = null;
    }

    /**
     * Marks as expired a code whose time is over. Meant to be called through {@code CooperativeLicense.expireCode},
     * which gives its seat and hectares back.
     *
     * @param at the instant to check
     * @return true if the code was available and is now expired; false if nothing changed
     */
    boolean expire(Instant at) {
        if (!isOverdue(at)) {
            return false;
        }
        this.status = InvitationCodeStatus.EXPIRED;
        this.sealedCode = null;
        return true;
    }

    /**
     * Returns the current state of the code.
     *
     * @return the snapshot
     */
    public InvitationCodeSnapshot snapshot() {
        return new InvitationCodeSnapshot(id, licenseId, batchId, fingerprint, quota, status, expiresAt, redeemedBy,
                redeemedAt, revision, hint, sealedCode, issuedAt, seatHeldUntil);
    }

    /**
     * What a new code of a batch keeps of its value: its fingerprint to find it, its last two characters to show it
     * masked, and the code encrypted to share it again while it can be redeemed.
     *
     * @param fingerprint the fingerprint of the code
     * @param hint        the last two characters of the code
     * @param sealedCode  the code encrypted
     */
    public record IssuedCode(CodeFingerprint fingerprint, String hint, String sealedCode) {
    }
}
