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
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money;
import com.arcadiadevs.viora.platform.subscription.domain.model.events.SubscriptionActivated;

/**
 * Aggregate root of a producer's right to use Viora, for a surface in hectares and a window of time.
 * <p>Sponsored subscriptions retain their invitation-code activation and license term. Individual subscriptions
 * begin pending and receive a calendar-year term only after verified payment approval.</p>
 */
public class Subscription extends AbstractDomainAggregateRoot<Subscription> {

    private final SubscriptionId id;
    private final ProducerId producerId;
    private final PlanMode mode;
    private SubscriptionStatus status;
    private final HectaresQuota quota;
    private SubscriptionPeriod period;
    private final CooperativeId cooperativeId;
    private final CooperativeLicenseId licenseId;
    private final InvitationCodeId redeemedCodeId;
    private final Long revision;
    private final String planCode;
    private final SubscriptionPrice price;
    private String paymentId;
    private final Instant createdAt;

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
        this.planCode = snapshot.planCode();
        this.price = snapshot.price();
        this.paymentId = snapshot.paymentId();
        this.createdAt = snapshot.createdAt();
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
                || snapshot.status() == null || snapshot.quota() == null) {
            throw new IllegalArgumentException("subscription.snapshot.incomplete");
        }
        validateModeState(snapshot);
        return new Subscription(snapshot);
    }

    private static void validateModeState(SubscriptionSnapshot snapshot) {
        if (snapshot.mode() == PlanMode.PLAN_COOPERATIVO_PATROCINADO) {
            if (snapshot.period() == null || snapshot.cooperativeId() == null || snapshot.licenseId() == null
                    || snapshot.redeemedCodeId() == null || snapshot.price() != null
                    || snapshot.planCode() != null || snapshot.paymentId() != null) {
                throw new IllegalArgumentException("subscription.snapshot.incomplete");
            }
            return;
        }
        if (snapshot.planCode() == null || snapshot.planCode().isBlank() || snapshot.price() == null
                || snapshot.createdAt() == null || snapshot.cooperativeId() != null
                || snapshot.licenseId() != null || snapshot.redeemedCodeId() != null) {
            throw new IllegalArgumentException("subscription.snapshot.incomplete");
        }
        new IndividualHectaresQuota(snapshot.quota().hectares());
        if (snapshot.status() == SubscriptionStatus.PENDING_PAYMENT) {
            if (snapshot.period() != null || snapshot.paymentId() != null) {
                throw new IllegalArgumentException("subscription.snapshot.incomplete");
            }
        } else if (snapshot.status() == SubscriptionStatus.ACTIVE || snapshot.status() == SubscriptionStatus.EXPIRED) {
            if (snapshot.period() == null || snapshot.paymentId() == null || snapshot.paymentId().isBlank()
                    || snapshot.period().startsAt().isBefore(snapshot.createdAt())
                    || !SubscriptionPeriod.annual(snapshot.period().startsAt()).equals(snapshot.period())) {
                throw new IllegalArgumentException("subscription.snapshot.incomplete");
            }
        }
    }

    /**
     * Creates an individual contract without granting access or contacting a payment provider.
     * @param producerId verified producer identity
     * @param planCode selected offer
     * @param quota validated individual capacity
     * @param price server-owned immutable terms
     * @param at creation instant
     * @return pending individual contract
     * @throws IllegalArgumentException if the creation state is incomplete
     */
    public static Subscription createIndividual(ProducerId producerId, String planCode,
            IndividualHectaresQuota quota, SubscriptionPrice price, Instant at) {
        if (producerId == null || quota == null) {
            throw new IllegalArgumentException("subscription.snapshot.incomplete");
        }
        var snapshot = new SubscriptionSnapshot(new SubscriptionId(), producerId,
                PlanMode.PLAN_PRODUCTOR_INDIVIDUAL, SubscriptionStatus.PENDING_PAYMENT,
                new HectaresQuota(quota.value()), null, null, null, null, 0L, planCode, price, null, at);
        validateModeState(snapshot);
        return new Subscription(snapshot);
    }

    /**
     * Activates only an individual contract using verified payment evidence.
     * @param paymentId provider payment identity
     * @param amount verified total
     * @param currency verified settlement currency
     * @param approvedAt verified approval time
     * @throws IllegalArgumentException if evidence does not match the contract
     * @throws IllegalStateException if the transition is invalid
     */
    public void activateIndividual(String paymentId, Money amount, String currency, Instant approvedAt) {
        if (mode != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL || paymentId == null || paymentId.isBlank()
                || amount == null || !"PEN".equals(currency) || !price.total().equals(amount)) {
            throw new IllegalArgumentException("subscription.payment.mismatch");
        }
        if (this.paymentId != null) {
            if (!this.paymentId.equals(paymentId)) {
                throw new IllegalStateException("subscription.payment.already_approved");
            }
            return;
        }
        if (status != SubscriptionStatus.PENDING_PAYMENT || approvedAt == null || approvedAt.isBefore(createdAt)) {
            throw new IllegalStateException("subscription.activation.invalid");
        }
        period = SubscriptionPeriod.annual(approvedAt);
        status = SubscriptionStatus.ACTIVE;
        this.paymentId = paymentId;
        registerDomainEvent(new SubscriptionActivated(id.subscriptionId(), producerId.producerId(),
                period.startsAt(), period.endsAt()));
    }

    /**
     * Checks whether the producer can use Viora under this subscription.
     *
     * @param at the instant to check
     * @return true if the subscription is active and {@code at} falls inside its window
     */
    public boolean hasActiveEntitlement(Instant at) {
        return status == SubscriptionStatus.ACTIVE && period != null && period.contains(at);
    }

    /**
     * Returns the current state of the subscription.
     *
     * @return the snapshot
     */
    public SubscriptionSnapshot snapshot() {
        return new SubscriptionSnapshot(id, producerId, mode, status, quota, period, cooperativeId, licenseId,
                redeemedCodeId, revision, planCode, price, paymentId, createdAt);
    }
}
