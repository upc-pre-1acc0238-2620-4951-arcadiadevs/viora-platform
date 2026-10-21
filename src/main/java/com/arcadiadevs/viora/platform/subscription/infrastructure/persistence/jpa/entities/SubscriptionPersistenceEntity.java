package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;
import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
/** Relational representation, separate from the domain contract. */
@Entity @Table(name = "subscription_contracts", indexes = @Index(name = "ix_subscription_owner", columnList = "producer_id"))
@Getter @Setter @NoArgsConstructor
public class SubscriptionPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "producer_id", nullable = false, length = 128) private String producerId;
    @Column(nullable = false) private String planCode;
    @Column(nullable = false, precision = 19, scale = 1) private BigDecimal quotaHa;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal baseAmount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal taxAmount;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false) private String status;
    private Instant startsAt;
    private Instant endsAt;
    @Column(unique = true) private String paymentId;
    @Column(nullable = false) private Instant requestedAt;
    @Version private Long revision;
}
