package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import java.util.Optional;
import java.util.UUID;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
/** Framework-free contract persistence port. */
public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(UUID id);
    Optional<Subscription> findCurrent(String producerId);
}
