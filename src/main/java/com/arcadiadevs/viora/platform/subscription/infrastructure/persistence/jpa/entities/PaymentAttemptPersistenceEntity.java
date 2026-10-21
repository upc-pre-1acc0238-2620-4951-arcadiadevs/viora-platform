package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import jakarta.persistence.*;
import lombok.*;
import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
/** Provider attempt data; credentials are never persisted here. */
@Entity @Table(name = "subscription_payment_attempts", indexes = @Index(columnList = "subscription_id"))
@Getter @Setter @NoArgsConstructor
public class PaymentAttemptPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "subscription_id", nullable = false) private UUID subscriptionId;
    @Column(nullable = false) private String producerId;
    @Column(nullable = false) private String status;
    @Column(unique = true) private String preferenceId;
    @Column(length = 4096) private String checkoutUrl;
    @Column(nullable = false) private Instant initiatedAt;
    @Column(nullable = false) private Instant expiresAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_payment_observations", joinColumns = @JoinColumn(name = "attempt_id"))
    @MapKeyColumn(name = "payment_id")
    @Column(name = "payment_status", nullable = false)
    private Map<String, String> paymentStates = new HashMap<>();
    @Version private Long revision;
}
