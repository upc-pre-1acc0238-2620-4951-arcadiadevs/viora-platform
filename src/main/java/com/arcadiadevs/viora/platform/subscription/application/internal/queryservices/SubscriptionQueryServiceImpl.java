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
import com.arcadiadevs.viora.platform.subscription.domain.repositories.PaymentAttemptRepository;

/** Resolves owned contracts without provider calls or client-supplied ownership. */
@Service
@Transactional(readOnly = true)
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {
    private final SubscriptionRepository subscriptions;
    private final Clock clock;
    private final PaymentAttemptRepository attempts;

    /**
     * Creates the contract read use case.
     * @param subscriptions shared persistence port
     * @param clock application time
     * @param attempts persisted payment evidence
     */
    public SubscriptionQueryServiceImpl(SubscriptionRepository subscriptions, Clock clock,
            PaymentAttemptRepository attempts) {
        this.subscriptions = subscriptions;
        this.clock = clock;
        this.attempts = attempts;
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

    /**
     * Reads persisted evidence without provider calls or foreign contract disclosure.
     * @param owner verified producer identity
     * @param id requested contract identifier
     * @return latest persisted attempt state, or null when unavailable
     * @throws IllegalArgumentException if the producer identifier is invalid
     */
    @Override
    public String paymentStatus(String owner, UUID id) {
        if (find(owner, id).isEmpty()) {
            return null;
        }
        return attempts.latest(id).filter(attempt -> owner.equals(attempt.producerId()))
                .map(attempt -> attempt.status()).orElse(null);
    }

}
