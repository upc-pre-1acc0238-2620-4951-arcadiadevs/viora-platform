package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters.CalibrationMultiplierPersistenceConverter;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters.DeviceNamePersistenceConverter;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters.PlotIdPersistenceConverter;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters.SensorDepthPersistenceConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity mapping the {@code telemetry.iot_devices} relational database table.
 */
@Entity
@Table(name = "iot_devices", schema = "telemetry")
@Getter
@Setter
@NoArgsConstructor
public class IoTDevicePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Convert(converter = PlotIdPersistenceConverter.class)
    @Column(name = "plot_id", nullable = false)
    private PlotId plotId;

    @Convert(converter = DeviceNamePersistenceConverter.class)
    @Column(name = "name", nullable = false, length = 100)
    private DeviceName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DeviceType type;

    @Convert(converter = SensorDepthPersistenceConverter.class)
    @Column(name = "depth_cm")
    private SensorDepth depth;

    @Enumerated(EnumType.STRING)
    @Column(name = "soil_texture_type", length = 30)
    private SoilTextureType soilTextureType;

    @Convert(converter = CalibrationMultiplierPersistenceConverter.class)
    @Column(name = "calibration_multiplier", nullable = false)
    private CalibrationMultiplier calibrationMultiplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeviceStatus status;

    @Column(name = "last_reading_timestamp")
    private Instant lastReadingTimestamp;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
