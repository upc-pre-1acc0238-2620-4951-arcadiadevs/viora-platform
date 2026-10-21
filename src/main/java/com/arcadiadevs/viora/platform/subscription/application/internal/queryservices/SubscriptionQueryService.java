package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;

/** Resolves owned contracts without provider calls or client-supplied ownership. */
@Service
public class SubscriptionQueryService {
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    /**
     * Creates the contract read use case.
     * @param subscriptions shared persistence port
     * @param clock application time
     */
    public SubscriptionQueryService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /**
     * Resolves current access or the latest purchase for a verified producer.
     * @param owner verified identity
     * @return current contract when present
     */
    public Optional<Subscription> current(String owner) {
        return subscriptions.findCurrent(new ProducerId(owner), clock.instant());
    }
}
