package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code cooperative_licenses} table.
 */
@Entity
@Table(name = "cooperative_licenses")
@Getter
@Setter
@NoArgsConstructor
public class CooperativeLicensePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "cooperative_id", nullable = false)
    private UUID cooperativeId;

    @Column(name = "cooperative_name", nullable = false, length = 120)
    private String cooperativeName;

    @Column(name = "seats", nullable = false)
    private int seats;

    @Column(name = "contracted_area_ha", nullable = false, precision = 8, scale = 2)
    private BigDecimal contractedAreaHa;

    @Column(name = "committed_area_ha", nullable = false, precision = 8, scale = 2)
    private BigDecimal committedAreaHa;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
