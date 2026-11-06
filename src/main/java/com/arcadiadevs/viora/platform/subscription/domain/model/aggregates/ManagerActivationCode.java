package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.ManagerCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

import java.time.Instant;

/**
 * Aggregate root of a manager activation code: the single-use code Viora hands to the organization that contracted a
 * license, so its technical manager can take charge of it.
 * <p>It is its own root because it lives before anyone holds the license, and because a new one replaces the
 * manager when the organization changes it. Only its fingerprint is kept. A code moves from {@code AVAILABLE} to
 * {@code REDEEMED}, {@code EXPIRED} or {@code REVOKED}, and never back.</p>
 */
public class ManagerActivationCode extends AbstractDomainAggregateRoot<ManagerActivationCode> {

    private final ManagerCodeId id;
    private final CooperativeLicenseId licenseId;
    private final CodeFingerprint fingerprint;
    private ManagerCodeStatus status;
    private final Instant expiresAt;
    private ManagerId redeemedBy;
    private Instant redeemedAt;
    private final Long revision;

    private ManagerActivationCode(ManagerActivationCodeSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.fingerprint = snapshot.fingerprint();
        this.status = snapshot.status();
        this.expiresAt = snapshot.expiresAt();
        this.redeemedBy = snapshot.redeemedBy();
        this.redeemedAt = snapshot.redeemedAt();
        this.revision = snapshot.revision();
    }

    /**
     * Issues a new available code of a license. Only {@link CooperativeLicense#issueManagerCode} calls it, so the code
     * never outlives its license.
     *
     * @param licenseId   the license the code hands over
     * @param fingerprint the fingerprint of the code
     * @param expiresAt   the last instant the code can be redeemed, exclusive
     * @return the available code
     * @throws IllegalArgumentException if any argument is null
     */
    static ManagerActivationCode issue(CooperativeLicenseId licenseId, CodeFingerprint fingerprint, Instant expiresAt) {
        return reconstitute(new ManagerActivationCodeSnapshot(new ManagerCodeId(), licenseId, fingerprint,
                ManagerCodeStatus.AVAILABLE, expiresAt, null, null, 0L));
    }

    /**
     * Rebuilds a code from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the code
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static ManagerActivationCode reconstitute(ManagerActivationCodeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.manager_code.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.fingerprint() == null
                || snapshot.status() == null || snapshot.expiresAt() == null) {
            throw new IllegalArgumentException("subscription.manager_code.snapshot.incomplete");
        }
        if (snapshot.status() == ManagerCodeStatus.REDEEMED
                && (snapshot.redeemedBy() == null || snapshot.redeemedAt() == null)) {
            throw new IllegalArgumentException("subscription.manager_code.snapshot.redemption_incomplete");
        }
        return new ManagerActivationCode(snapshot);
    }

    /**
     * Checks whether the code can still be redeemed.
     *
     * @param at the instant of the redemption
     * @return true if the code is available and has not expired at {@code at}
     */
    public boolean isRedeemable(Instant at) {
        return status == ManagerCodeStatus.AVAILABLE && at != null && at.isBefore(expiresAt);
    }

    /**
     * Marks the code as redeemed by a manager. Meant to be called through
     * {@link CooperativeLicense#assignManager}.
     *
     * @param manager the manager redeeming the code
     * @param at      the instant of the redemption
     * @throws IllegalArgumentException           if an argument is null
     * @throws ManagerCodeNotRedeemableException if the code is not available or has expired
     */
    void redeem(ManagerId manager, Instant at) {
        if (manager == null || at == null) {
            throw new IllegalArgumentException("subscription.manager_code.redemption.incomplete");
        }
        if (!isRedeemable(at)) {
            throw new ManagerCodeNotRedeemableException();
        }
        this.status = ManagerCodeStatus.REDEEMED;
        this.redeemedBy = manager;
        this.redeemedAt = at;
    }

    /**
     * Withdraws a code nobody redeemed yet, such as one the organization lost; Viora then issues another.
     *
     * @throws BusinessRuleException if the code is no longer available
     */
    public void revoke() {
        if (status != ManagerCodeStatus.AVAILABLE) {
            throw new BusinessRuleException("subscription.manager_code.not_revocable");
        }
        this.status = ManagerCodeStatus.REVOKED;
    }

    /**
     * Returns the current state of the code.
     *
     * @return the snapshot
     */
    public ManagerActivationCodeSnapshot snapshot() {
        return new ManagerActivationCodeSnapshot(id, licenseId, fingerprint, status, expiresAt, redeemedBy,
                redeemedAt, revision);
    }
}
