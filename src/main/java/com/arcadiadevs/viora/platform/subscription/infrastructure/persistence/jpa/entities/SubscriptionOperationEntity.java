package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
/** Durable request replay record scoped by owner and idempotency key. */
@Entity @Table(name = "subscription_operations", uniqueConstraints = @UniqueConstraint(columnNames = {"producer_id", "operation_key"}))
@Getter @Setter @NoArgsConstructor
public class SubscriptionOperationEntity {
    @Id private UUID id;
    @Column(name = "producer_id", nullable = false, length = 128) private String producerId;
    @Column(name = "operation_key", nullable = false, length = 128) private String operationKey;
    @Column(nullable = false) private String kind;
    @Column(nullable = false, length = 512) private String fingerprint;
    @Column(nullable = false) private UUID resourceId;
}
