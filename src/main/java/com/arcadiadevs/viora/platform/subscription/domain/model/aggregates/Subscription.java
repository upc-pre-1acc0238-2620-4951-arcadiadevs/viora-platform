package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import java.time.Instant;
import java.util.UUID;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.events.SubscriptionActivated;
/** Initial individual contract. Checkout creation never activates an entitlement. */
public final class Subscription extends AbstractDomainAggregateRoot<Subscription> {
    private SubscriptionSnapshot state;
    private Subscription(SubscriptionSnapshot state) { this.state = state; }
    /** @param producerId owner @param planCode catalogue code @param quota capacity @param price quote @param now clock instant @return pending contract */
    public static Subscription create(String producerId, String planCode, HectaresQuota quota, SubscriptionPrice price, Instant now) {
        if (producerId == null || producerId.isBlank() || planCode == null || planCode.isBlank()
                || quota == null || price == null || now == null) {
            throw new IllegalArgumentException("subscription.contract.invalid");
        }
        return new Subscription(new SubscriptionSnapshot(UUID.randomUUID(), producerId, planCode, quota, price,
                "PENDING_PAYMENT", null, null, now));
    }
    /** @param snapshot persisted state @return aggregate */
    public static Subscription reconstitute(SubscriptionSnapshot snapshot) {
        if (snapshot == null || snapshot.id() == null) {
            throw new IllegalArgumentException("subscription.snapshot.invalid");
        }
        return new Subscription(snapshot);
    }
    /** @return immutable state */
    public SubscriptionSnapshot snapshot() { return state; }
    /** @param paymentId verified provider transaction @param amount verified amount @param currency verified currency @param approvedAt verified time */
    public void activate(String paymentId, Money amount, String currency, Instant approvedAt) {
        if (paymentId == null || paymentId.isBlank() || !"PEN".equals(currency) || amount == null
                || !state.price().total().equals(amount)) {
            throw new IllegalArgumentException("subscription.payment.mismatch");
        }
        if (state.paymentId() != null) {
            if (!state.paymentId().equals(paymentId)) {
                throw new IllegalStateException("subscription.payment.already_approved");
            }
            return;
        }
        if (!"PENDING_PAYMENT".equals(state.status()) || approvedAt == null || approvedAt.isBefore(state.createdAt())) {
            throw new IllegalStateException("subscription.activation.invalid");
        }
        var period = SubscriptionPeriod.annual(approvedAt);
        state = new SubscriptionSnapshot(state.id(), state.producerId(), state.planCode(), state.quota(), state.price(),
                "ACTIVE", period, paymentId, state.createdAt());
        registerDomainEvent(new SubscriptionActivated(state.id(), state.producerId(), period.startsAt(), period.endsAt()));
    }
    /** @param at evaluation time @return current entitlement, independently of scheduled jobs */
    public boolean hasActiveEntitlement(Instant at) {
        return "ACTIVE".equals(state.status()) && state.period() != null && state.period().contains(at);
    }
}
