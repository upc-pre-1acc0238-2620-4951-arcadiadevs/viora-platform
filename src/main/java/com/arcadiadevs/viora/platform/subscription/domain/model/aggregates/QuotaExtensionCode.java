package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;

import java.time.Instant;

/**
 * Aggregate root of an extension code: a code a cooperative shares with all its producers so each one can add the
 * same hectares to the plan the cooperative sponsors.
 * <p>Unlike an {@link InvitationCode} it is not consumed: every producer of its license can redeem it once, and each
 * redemption commits its hectares on the license at that moment, so a code stops working when the license runs out of
 * hectares. Only its fingerprint is kept.</p>
 */
public class QuotaExtensionCode extends AbstractDomainAggregateRoot<QuotaExtensionCode> {

    private final ExtensionCodeId id;
    private final CooperativeLicenseId licenseId;
    private final CodeFingerprint fingerprint;
    private final HectaresQuota addedArea;
    private final ExtensionCodeStatus status;
    private final Instant expiresAt;
    private final Long revision;

    private QuotaExtensionCode(QuotaExtensionCodeSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.fingerprint = snapshot.fingerprint();
        this.addedArea = snapshot.addedArea();
        this.status = snapshot.status();
        this.expiresAt = snapshot.expiresAt();
        this.revision = snapshot.revision();
    }

    /**
     * Issues a new active code of a license. Only {@link CooperativeLicense#issueExtensionCode} calls it.
     *
     * @param licenseId   the license the code draws hectares from
     * @param fingerprint the fingerprint of the code
     * @param addedArea   the hectares each producer gets
     * @param expiresAt   the last instant the code can be redeemed, exclusive
     * @return the active code
     * @throws IllegalArgumentException if any argument is null
     */
    static QuotaExtensionCode issue(CooperativeLicenseId licenseId, CodeFingerprint fingerprint,
                                    HectaresQuota addedArea, Instant expiresAt) {
        return reconstitute(new QuotaExtensionCodeSnapshot(new ExtensionCodeId(), licenseId, fingerprint, addedArea,
                ExtensionCodeStatus.ACTIVE, expiresAt, 0L));
    }

    /**
     * Rebuilds a code from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the code
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static QuotaExtensionCode reconstitute(QuotaExtensionCodeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.extension_code.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.fingerprint() == null
                || snapshot.addedArea() == null || snapshot.status() == null || snapshot.expiresAt() == null) {
            throw new IllegalArgumentException("subscription.extension_code.snapshot.incomplete");
        }
        return new QuotaExtensionCode(snapshot);
    }

    /**
     * Checks whether the code can still be redeemed.
     *
     * @param at the instant of the redemption
     * @return true if the code is active and has not expired at {@code at}
     */
    public boolean isRedeemable(Instant at) {
        return status == ExtensionCodeStatus.ACTIVE && at != null && at.isBefore(expiresAt);
    }

    /**
     * Returns the current state of the code.
     *
     * @return the snapshot
     */
    public QuotaExtensionCodeSnapshot snapshot() {
        return new QuotaExtensionCodeSnapshot(id, licenseId, fingerprint, addedArea, status, expiresAt, revision);
    }
}
