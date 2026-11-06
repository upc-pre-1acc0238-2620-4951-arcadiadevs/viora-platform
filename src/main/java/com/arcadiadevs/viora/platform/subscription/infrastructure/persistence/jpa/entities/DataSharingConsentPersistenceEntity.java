package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code data_sharing_consents} table: who accepted to share their data with a license, when
 * and which version of the text.
 */
@Entity
@Table(name = "data_sharing_consents")
@Getter
@Setter
@NoArgsConstructor
public class DataSharingConsentPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "producer_id", nullable = false)
    private UUID producerId;

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "text_version", nullable = false, length = 40)
    private String textVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
