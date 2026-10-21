package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;

/** Resolves owned contracts without provider calls or client-supplied ownership. */
@Service
@Transactional(readOnly = true)
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    /**
     * Creates the contract read use case.
     * @param subscriptions shared persistence port
     * @param clock application time
     */
    public SubscriptionQueryServiceImpl(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /**
     * Resolves current access or the latest purchase for a verified producer.
     * @param owner verified identity
     * @return current contract when present
     */
    @Override
    public Optional<Subscription> current(String owner) {
        return subscriptions.findCurrent(new ProducerId(owner), clock.instant());
    }
    /**
     * Finds a contract only when the authenticated identity owns it.
     * @param owner verified actor
     * @param id requested identity
     * @return owned contract or an empty result
     */
    @Override
    public Optional<Subscription> find(String owner, UUID id) {
        var actor = new ProducerId(owner);
        return subscriptions.findByIdAndProducerId(new SubscriptionId(id.toString()), actor);
    }

}
