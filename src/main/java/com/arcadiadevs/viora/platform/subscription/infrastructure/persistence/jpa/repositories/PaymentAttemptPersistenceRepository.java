package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.PaymentAttemptPersistenceEntity;
public interface PaymentAttemptPersistenceRepository extends JpaRepository<PaymentAttemptPersistenceEntity, UUID> {
    Optional<PaymentAttemptPersistenceEntity> findFirstBySubscriptionIdOrderByInitiatedAtDesc(UUID subscriptionId);
}
