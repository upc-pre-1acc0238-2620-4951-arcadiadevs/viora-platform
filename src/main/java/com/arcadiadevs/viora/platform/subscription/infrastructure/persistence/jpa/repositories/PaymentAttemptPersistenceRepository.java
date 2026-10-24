package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.PaymentAttemptPersistenceEntity;
public interface PaymentAttemptPersistenceRepository extends JpaRepository<PaymentAttemptPersistenceEntity, UUID> {
    Optional<PaymentAttemptPersistenceEntity> findFirstBySubscriptionIdAndExtensionIdIsNullOrderByInitiatedAtDesc(UUID subscriptionId);
    Optional<PaymentAttemptPersistenceEntity> findFirstByExtensionIdOrderByInitiatedAtDesc(UUID extensionId);

    /** Everything of a deleted account, for the account purge. */
    List<PaymentAttemptPersistenceEntity> findAllByProducerId(String producerId);
}
