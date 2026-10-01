package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code telemetry.hourly_telemetry_readings} relational database table.
 */
@Entity
@Table(name = "hourly_telemetry_readings")
@Getter
@Setter
@NoArgsConstructor
public class HourlyTelemetryReadingPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private TelemetrySeriesPersistenceEntity series;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "reading_timestamp", nullable = false)
    private Instant readingTimestamp;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "relative_humidity", nullable = false)
    private Double relativeHumidity;

    @Column(name = "soil_moisture", nullable = false)
    private Double soilMoisture;

    @Column(name = "solar_radiation", nullable = false)
    private Double solarRadiation;

    @Column(name = "stem_water_potential")
    private Double stemWaterPotential;
}
