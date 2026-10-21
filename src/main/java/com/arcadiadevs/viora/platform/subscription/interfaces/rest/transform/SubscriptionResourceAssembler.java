package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.SubscriptionResource;

import java.time.Instant;

/**
 * Assembler from the {@link Subscription} aggregate to its public resource.
 */
public final class SubscriptionResourceAssembler {

    private SubscriptionResourceAssembler() {
    }

    /**
     * Builds the public resource of a subscription.
     *
     * @param subscription    the subscription
     * @param cooperativeName the name of the sponsoring cooperative, or null
     * @param at              the instant used to tell whether the entitlement is active
     * @return the resource
     */
    public static SubscriptionResource toResource(Subscription subscription, String cooperativeName, Instant at) {
        var snapshot = subscription.snapshot();
        return new SubscriptionResource(
                snapshot.id().subscriptionId(),
                snapshot.mode().name(),
                snapshot.status().name(),
                snapshot.quota().hectares(),
                snapshot.period().startsAt(),
                snapshot.period().endsAt(),
                subscription.hasActiveEntitlement(at),
                snapshot.cooperativeId() == null ? null : snapshot.cooperativeId().cooperativeId(),
                cooperativeName);
    }
}
