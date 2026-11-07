package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.DataSharingConsentNotActiveException;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ConsentTextVersion;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.RedeemedCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;

import java.time.Instant;

/**
 * Aggregate root of the consent a producer gives, when redeeming a cooperative code, to let the manager of the license
 * read their data: who accepted, when and which version of the text.
 * <p>It is its own root because it is a record that has to outlive changes of the plan, and because the manager's
 * read access will ask it alone. The data is shared with the license, so a new manager reads it too. Reading lasts
 * while the producer shares and the sponsored plan lasts; stopping is for good and leaves the plan as it is.</p>
 */
public class DataSharingConsent extends AbstractDomainAggregateRoot<DataSharingConsent> {

    private final DataSharingConsentId id;
    private final ProducerId producerId;
    private final CooperativeLicenseId licenseId;
    private final SubscriptionId subscriptionId;
    private final ManagerId managerId;
    private final ConsentTextVersion textVersion;
    private final Instant acceptedAt;
    private Instant endsAt;
    private Instant withdrawnAt;
    private final Long revision;

    private DataSharingConsent(DataSharingConsentSnapshot snapshot) {
        this.id = snapshot.id();
        this.producerId = snapshot.producerId();
        this.licenseId = snapshot.licenseId();
        this.subscriptionId = snapshot.subscriptionId();
        this.managerId = snapshot.managerId();
        this.textVersion = snapshot.textVersion();
        this.acceptedAt = snapshot.acceptedAt();
        this.endsAt = snapshot.endsAt();
        this.withdrawnAt = snapshot.withdrawnAt();
        this.revision = snapshot.revision();
    }

    /**
     * Records the consent of a producer who redeemed a code of a license, accepting a version of the text. It lasts
     * as long as the sponsored plan.
     *
     * @param redeemed    the evidence of the redemption, with the producer and the license
     * @param plan        the sponsored plan the redemption activated
     * @param manager     the manager in charge of the license at that moment; null if it had none
     * @param textVersion the version of the text the producer accepted
     * @param at          the instant of the acceptance
     * @return the active consent
     * @throws IllegalArgumentException if a required argument is null or the plan is not the one redeemed
     */
    public static DataSharingConsent accept(RedeemedCode redeemed, Subscription plan, ManagerId manager,
                                            ConsentTextVersion textVersion, Instant at) {
        if (redeemed == null || plan == null || textVersion == null || at == null) {
            throw new IllegalArgumentException("subscription.consent.acceptance.incomplete");
        }
        var planSnapshot = plan.snapshot();
        if (!redeemed.producerId().equals(planSnapshot.producerId())
                || !redeemed.licenseId().equals(planSnapshot.licenseId())) {
            throw new IllegalArgumentException("subscription.consent.plan_of_another_redemption");
        }
        return reconstitute(new DataSharingConsentSnapshot(new DataSharingConsentId(), redeemed.producerId(),
                redeemed.licenseId(), planSnapshot.id(), manager, textVersion, at, planSnapshot.period().endsAt(),
                null, 0L));
    }

    /**
     * Rebuilds a consent from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the consent
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static DataSharingConsent reconstitute(DataSharingConsentSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.consent.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.producerId() == null || snapshot.licenseId() == null
                || snapshot.subscriptionId() == null || snapshot.textVersion() == null
                || snapshot.acceptedAt() == null || snapshot.endsAt() == null) {
            throw new IllegalArgumentException("subscription.consent.snapshot.incomplete");
        }
        return new DataSharingConsent(snapshot);
    }

    /**
     * Tells whether the manager of the license can read the producer's data at an instant.
     *
     * @param at the instant to check
     * @return {@code WITHDRAWN} once the producer stopped sharing, {@code ENDED} once the plan ended, otherwise
     *         {@code ACTIVE}
     * @throws IllegalArgumentException if {@code at} is null
     */
    public DataSharingConsentStatus statusAt(Instant at) {
        if (at == null) {
            throw new IllegalArgumentException("subscription.consent.instant.null");
        }
        if (withdrawnAt != null) {
            return DataSharingConsentStatus.WITHDRAWN;
        }
        return at.isBefore(endsAt) ? DataSharingConsentStatus.ACTIVE : DataSharingConsentStatus.ENDED;
    }

    /**
     * Stops sharing for good: the manager stops reading the producer's data at once. The plan and its seat stay.
     *
     * @param at the instant the producer stops sharing
     * @throws IllegalArgumentException             if {@code at} is null
     * @throws DataSharingConsentNotActiveException if the producer already stopped or the plan ended
     */
    public void withdraw(Instant at) {
        if (statusAt(at) != DataSharingConsentStatus.ACTIVE) {
            throw new DataSharingConsentNotActiveException();
        }
        this.withdrawnAt = at;
    }

    /**
     * Ends the manager's reading at once because the member was released from the license. Unlike a withdrawal, it is
     * not the member's choice, so the consent ends instead of being withdrawn.
     *
     * @param at the instant of the release
     * @return true if the consent was active and ended now
     */
    public boolean endWithRelease(Instant at) {
        if (at == null || statusAt(at) != DataSharingConsentStatus.ACTIVE) {
            return false;
        }
        this.endsAt = at;
        return true;
    }

    /**
     * Extends an active consent with its renewed plan. A withdrawn or ended consent does not come back.
     *
     * @param newEndsAt the plan's new end
     * @param at        the instant of the renewal
     * @return true if the consent now lasts longer
     */
    public boolean extendTo(Instant newEndsAt, Instant at) {
        if (newEndsAt == null || at == null || statusAt(at) != DataSharingConsentStatus.ACTIVE
                || !newEndsAt.isAfter(endsAt)) {
            return false;
        }
        this.endsAt = newEndsAt;
        return true;
    }

    /**
     * Returns the current state of the consent.
     *
     * @return the snapshot
     */
    public DataSharingConsentSnapshot snapshot() {
        return new DataSharingConsentSnapshot(id, producerId, licenseId, subscriptionId, managerId, textVersion,
                acceptedAt, endsAt, withdrawnAt, revision);
    }
}
