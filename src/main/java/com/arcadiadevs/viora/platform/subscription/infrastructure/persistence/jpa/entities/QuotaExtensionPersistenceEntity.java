package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionSource;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code subscription_quota_extensions} table. A producer can redeem an extension code only
 * once, which the unique constraint also guards.
 */
@Entity
@Table(name = "subscription_quota_extensions",
        indexes = @Index(columnList = "subscription_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"extension_code_id", "producer_id"}))
@Getter
@Setter
@NoArgsConstructor
public class QuotaExtensionPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "producer_id", nullable = false)
    private UUID producerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private QuotaExtensionSource source;

    @Column(name = "added_ha", nullable = false, precision = 8, scale = 2)
    private BigDecimal addedHa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QuotaExtensionStatus status;

    @Column(name = "billed_months")
    private Integer billedMonths;

    @Column(name = "base_amount", precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "extension_code_id")
    private UUID extensionCodeId;

    @Column(name = "payment_id", unique = true)
    private String paymentId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
