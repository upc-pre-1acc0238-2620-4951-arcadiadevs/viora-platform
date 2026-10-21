package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.model.events.CooperativeCodeRedeemedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.RedeemedCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;

import java.time.Instant;

/**
 * Aggregate root of a producer's right to use Viora, for a surface in hectares and a window of time.
 * <p>This first version only knows the mode sponsored by a cooperative: it is born active from a redeemed
 * invitation code. The individual paid mode is added on top of it.</p>
 */
public class Subscription extends AbstractDomainAggregateRoot<Subscription> {

    private final SubscriptionId id;
    private final ProducerId producerId;
    private final PlanMode mode;
    private SubscriptionStatus status;
    private final HectaresQuota quota;
    private final SubscriptionPeriod period;
    private final CooperativeId cooperativeId;
    private final CooperativeLicenseId licenseId;
    private final InvitationCodeId redeemedCodeId;
    private final Long revision;

    private Subscription(SubscriptionSnapshot snapshot) {
        this.id = snapshot.id();
        this.producerId = snapshot.producerId();
        this.mode = snapshot.mode();
        this.status = snapshot.status();
        this.quota = snapshot.quota();
        this.period = snapshot.period();
        this.cooperativeId = snapshot.cooperativeId();
        this.licenseId = snapshot.licenseId();
        this.redeemedCodeId = snapshot.redeemedCodeId();
        this.revision = snapshot.revision();
    }

    /**
     * Activates a subscription sponsored by a cooperative from a redeemed code. It is in force from {@code at} until
     * the license ends, and publishes {@link CooperativeCodeRedeemedEvent}.
     *
     * @param code the evidence of the redemption
     * @param at   the instant of the redemption
     * @return the active sponsored subscription
     * @throws IllegalArgumentException if an argument is null or the license has already ended at {@code at}
     */
    public static Subscription activateFromCode(RedeemedCode code, Instant at) {
        if (code == null || at == null) {
            throw new IllegalArgumentException("subscription.activation.incomplete");
        }
        var period = new SubscriptionPeriod(at, code.licensePeriod().endsAt());
        var subscription = new Subscription(new SubscriptionSnapshot(new SubscriptionId(), code.producerId(),
                PlanMode.PLAN_COOPERATIVO_PATROCINADO, SubscriptionStatus.ACTIVE, code.quota(), period,
                code.cooperativeId(), code.licenseId(), code.codeId(), 0L));
        subscription.registerDomainEvent(new CooperativeCodeRedeemedEvent(
                subscription.id.subscriptionId(),
                code.producerId().producerId(),
                code.cooperativeId().cooperativeId(),
                code.codeId().invitationCodeId(),
                at));
        return subscription;
    }

    /**
     * Rebuilds a subscription from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the subscription
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static Subscription reconstitute(SubscriptionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.producerId() == null || snapshot.mode() == null
                || snapshot.status() == null || snapshot.quota() == null || snapshot.period() == null) {
            throw new IllegalArgumentException("subscription.snapshot.incomplete");
        }
        return new Subscription(snapshot);
    }

    /**
     * Checks whether the producer can use Viora under this subscription.
     *
     * @param at the instant to check
     * @return true if the subscription is active and {@code at} falls inside its window
     */
    public boolean hasActiveEntitlement(Instant at) {
        return status == SubscriptionStatus.ACTIVE && period.contains(at);
    }

    /**
     * Returns the current state of the subscription.
     *
     * @return the snapshot
     */
    public SubscriptionSnapshot snapshot() {
        return new SubscriptionSnapshot(id, producerId, mode, status, quota, period, cooperativeId, licenseId,
                redeemedCodeId, revision);
    }
}
