package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.SubscriptionSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
/** Owner-scoped lookup; cached state never overrides time-based entitlement. */
public class SubscriptionQueryService {
    private final SubscriptionRepository subscriptions;
    private final Clock clock;
    /** @param subscriptions persistence port @param clock time */
    public SubscriptionQueryService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }
    /** @param owner verified actor @return latest contract, including a pending one */
    public Optional<SubscriptionSnapshot> current(String owner) {
        return subscriptions.findCurrent(owner).map(s -> s.snapshot());
    }
    /** @param owner verified actor @param id contract identifier @return owned contract or empty */
    public Optional<SubscriptionSnapshot> find(String owner, UUID id) {
        return subscriptions.findById(id).filter(s -> s.snapshot().producerId().equals(owner)).map(s -> s.snapshot());
    }
    /** @param snapshot contract @return whether current time is inside the approved period */
    public boolean entitled(SubscriptionSnapshot snapshot) {
        return "ACTIVE".equals(snapshot.status()) && snapshot.period() != null && snapshot.period().contains(clock.instant());
    }
}
