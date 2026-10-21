package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
public interface SubscriptionPersistenceRepository extends JpaRepository<SubscriptionPersistenceEntity, UUID> {
    Optional<SubscriptionPersistenceEntity> findFirstByProducerIdOrderByRequestedAtDesc(String producerId);
}
