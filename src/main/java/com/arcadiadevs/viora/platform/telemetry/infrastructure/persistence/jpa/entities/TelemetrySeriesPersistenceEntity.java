package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping the {@code telemetry.telemetry_series} relational database table.
 */
@Entity
@Table(name = "telemetry_series", schema = "telemetry")
@Getter
@Setter
@NoArgsConstructor
public class TelemetrySeriesPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "plot_id", nullable = false, unique = true)
    private UUID plotId;

    @Column(name = "sensor_node_id")
    private UUID sensorNodeId;

    @Column(name = "current_status", nullable = false, length = 30)
    private String currentStatus;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("readingTimestamp ASC")
    private List<HourlyTelemetryReadingPersistenceEntity> readings = new ArrayList<>();

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
