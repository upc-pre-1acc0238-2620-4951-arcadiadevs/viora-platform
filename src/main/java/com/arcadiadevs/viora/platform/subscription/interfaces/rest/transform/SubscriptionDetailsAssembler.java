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
        var s = subscription.snapshot();
        var active = subscription.hasActiveEntitlement(at);
        var status = s.status() == SubscriptionStatus.ACTIVE && !active
                ? SubscriptionStatus.EXPIRED : s.status();
        return new SubscriptionDetailsResource(s.id().subscriptionId(), s.mode().name(), s.planCode(), status.name(),
                s.quota().hectares(), s.price() == null ? null : "PEN",
                s.price() == null ? null : s.price().base().amount(),
                s.price() == null ? null : s.price().tax().amount(),
                s.price() == null ? null : s.price().total().amount(),
                s.period() == null ? null : s.period().startsAt(),
                s.period() == null ? null : s.period().endsAt(), active,
                s.cooperativeId() == null ? null : s.cooperativeId().cooperativeId());
    }
}
