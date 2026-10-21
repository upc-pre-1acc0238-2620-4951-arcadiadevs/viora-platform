package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code subscriptions} table. A redeemed code activates at most one subscription.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "producer_id", nullable = false)
    private UUID producerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 40)
    private PlanMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(name = "quota_ha", nullable = false, precision = 8, scale = 2)
    private BigDecimal quotaHa;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "cooperative_id")
    private UUID cooperativeId;

    @Column(name = "license_id")
    private UUID licenseId;

    @Column(name = "redeemed_code_id", unique = true)
    private UUID redeemedCodeId;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
