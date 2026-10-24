package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import java.time.Instant;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.SubscriptionDetailsResource;

/** Projects actual contract mode without manufacturing sponsored payment amounts. */
public final class SubscriptionDetailsAssembler {
    private SubscriptionDetailsAssembler() { }

    /**
     * Maps the shared contract without changing the existing cooperative endpoint resource.
     * @param subscription stored domain contract
     * @param at current instant
     * @return public details
     */
    public static SubscriptionDetailsResource toResource(Subscription subscription, Instant at) {
        return toResource(subscription, at, null);
    }

    /**
     * Maps an owned contract with its persisted payment attempt projection.
     * @param subscription owned contract
     * @param at entitlement evaluation time
     * @param paymentStatus persisted attempt state, nullable
     * @return public subscription details
     */
    public static SubscriptionDetailsResource toResource(Subscription subscription, Instant at, String paymentStatus) {
        var s = subscription.snapshot();
        var active = subscription.hasActiveEntitlement(at);
        // A renewal paid ahead stays ACTIVE until its year starts; only a window that already ended is EXPIRED.
        var ended = s.period() != null && !at.isBefore(s.period().endsAt());
        var status = s.status() == SubscriptionStatus.ACTIVE && !active && ended
                ? SubscriptionStatus.EXPIRED : s.status();
        return new SubscriptionDetailsResource(s.id().subscriptionId(), s.mode().name(), s.planCode(), status.name(),
                s.quota().hectares(), s.price() == null ? null : "PEN",
                s.price() == null ? null : s.price().base().amount(),
                s.price() == null ? null : s.price().tax().amount(),
                s.price() == null ? null : s.price().total().amount(),
                s.period() == null ? null : s.period().startsAt(),
                s.period() == null ? null : s.period().endsAt(), active,
                s.cooperativeId() == null ? null : s.cooperativeId().cooperativeId(), paymentStatus);
    }
}
