package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionProducerGate;
public interface SubscriptionGateRepository extends JpaRepository<SubscriptionProducerGate, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from SubscriptionProducerGate g where g.producerId = :owner")
    Optional<SubscriptionProducerGate> lock(@Param("owner") String owner);
}
