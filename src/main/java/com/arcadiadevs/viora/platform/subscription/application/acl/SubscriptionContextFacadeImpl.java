package com.arcadiadevs.viora.platform.subscription.application.acl;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

/** Implementation of the Subscription ACL facade, answering from the subscription in force. */
@Service
@Transactional(readOnly = true)
public class SubscriptionContextFacadeImpl implements SubscriptionContextFacade {

    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public SubscriptionContextFacadeImpl(SubscriptionRepository subscriptionRepository, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Override
    public Optional<BigDecimal> findEntitledQuotaHectares(String producerId) {
        final ProducerId id;
        try {
            id = new ProducerId(producerId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        var now = clock.instant();
        return subscriptionRepository.findCurrent(id, now)
                .filter(subscription -> subscription.hasActiveEntitlement(now))
                .map(subscription -> subscription.snapshot().quota().hectares());
    }
}
