package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionOperationEntity;
public interface SubscriptionOperationRepository extends JpaRepository<SubscriptionOperationEntity, UUID> {
    Optional<SubscriptionOperationEntity> findByProducerIdAndOperationKey(String producerId, String operationKey);

    /** Everything of a deleted account, for the account purge. */
    List<SubscriptionOperationEntity> findAllByProducerId(String producerId);
}
