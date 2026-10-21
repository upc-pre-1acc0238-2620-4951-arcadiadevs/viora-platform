package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import java.time.Instant;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.SubscriptionSnapshot;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.SubscriptionResource;
/** Maps pure contract state to the public wire format. */
public final class SubscriptionResourceAssembler {
    private SubscriptionResourceAssembler() { }
    public static SubscriptionResource toResource(SubscriptionSnapshot s, Instant now) {
        boolean active = "ACTIVE".equals(s.status()) && s.period() != null && s.period().contains(now);
        String status = "ACTIVE".equals(s.status()) && !active ? "EXPIRED" : s.status();
        return new SubscriptionResource(s.id(), "INDIVIDUAL_PAID", s.planCode(), status, s.quota().value(), "PEN",
                s.price().base().amount(), s.price().tax().amount(), s.price().total().amount(),
                s.period() == null ? null : s.period().startsAt(), s.period() == null ? null : s.period().endsAt(), active);
    }
}
