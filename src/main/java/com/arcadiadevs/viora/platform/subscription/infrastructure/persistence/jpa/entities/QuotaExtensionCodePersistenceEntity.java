package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeStatus;
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
 * JPA entity mapping the {@code subscription_extension_codes} table. The code itself is never stored, only its
 * fingerprint.
 */
@Entity
@Table(name = "subscription_extension_codes")
@Getter
@Setter
@NoArgsConstructor
public class QuotaExtensionCodePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "added_ha", nullable = false, precision = 8, scale = 2)
    private BigDecimal addedHa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExtensionCodeStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
