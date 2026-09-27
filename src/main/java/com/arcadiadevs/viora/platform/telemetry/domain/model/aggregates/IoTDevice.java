package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.telemetry.domain.exceptions.DeviceRevisionMismatchException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceCalibratedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceRegisteredEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceUnlinkedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Aggregate Root representing an IoT virtual sensor node installed in an olive orchard plot.
 * Manages sensor telemetry calibration factors, soil textural parameters, and operational status.
 *
 * <p>Invariant rules:
 * <ul>
 *   <li>Domain-assigned identity ({@link DeviceId}).</li>
 *   <li>Immutable state access exclusively via {@link #snapshot()}.</li>
 *   <li>Zero setters or persistence framework dependencies in domain.</li>
 *   <li>Soil probes must have an assigned installation depth.</li>
 * </ul>
 * </p>
 */
public class IoTDevice extends AbstractDomainAggregateRoot<IoTDevice> {

    private final DeviceId id;
    private final PlotId plotId;
    private DeviceName name;
    private DeviceType type;
    private SensorDepth depth;
    private SoilTextureType soilTextureType;
    private CalibrationMultiplier calibrationMultiplier;
    private DeviceStatus status;
    private Instant lastReadingTimestamp;
    private Long revision;

    private IoTDevice(
            DeviceId id,
            PlotId plotId,
            DeviceName name,
            DeviceType type,
            SensorDepth depth,
            SoilTextureType soilTextureType,
            CalibrationMultiplier calibrationMultiplier,
            DeviceStatus status,
            Instant lastReadingTimestamp,
            Long revision
    ) {
        this.id = id;
        this.plotId = plotId;
        this.name = name;
        this.type = type;
        this.depth = depth;
        this.soilTextureType = soilTextureType;
        this.calibrationMultiplier = calibrationMultiplier;
        this.status = status;
        this.lastReadingTimestamp = lastReadingTimestamp;
        this.revision = revision;
    }

    /**
     * Domain factory method that registers and binds a new virtual IoT device to an orchard plot.
     * Validates type-specific invariants and registers the {@link IoTDeviceRegisteredEvent}.
     *
     * @param plotId                the logical reference to the monitored plot
     * @param name                  the descriptive device name
     * @param type                  the functional device classification
     * @param depth                 the installation depth
     * @param soilTextureType       the soil textural classification
     * @param calibrationMultiplier the empirical adjustment factor (optional, defaults to 1.0)
     * @return a newly initialized and consistent {@link IoTDevice} aggregate root
     * @throws IllegalArgumentException if required fields are missing or type-specific invariants are violated
     */
    public static IoTDevice register(
            PlotId plotId,
            DeviceName name,
            DeviceType type,
            SensorDepth depth,
            SoilTextureType soilTextureType,
            CalibrationMultiplier calibrationMultiplier
    ) {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("device.name.blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("device.type.blank");
        }

        var effectiveMultiplier = (calibrationMultiplier != null)
                ? calibrationMultiplier
                : CalibrationMultiplier.defaultMultiplier();

        SensorDepth effectiveDepth;
        SoilTextureType effectiveTexture;

        if (type == DeviceType.SOIL_PROBE) {
            if (depth == null || !depth.isPresent()) {
                throw new IllegalArgumentException("device.soil_probe.depth_required");
            }
            effectiveDepth = depth;
            effectiveTexture = (soilTextureType != null && soilTextureType != SoilTextureType.NOT_APPLICABLE)
                    ? soilTextureType
                    : SoilTextureType.LOAM;
        } else {
            effectiveDepth = SensorDepth.none();
            effectiveTexture = SoilTextureType.NOT_APPLICABLE;
        }

        var deviceId = new DeviceId();
        var initialRevision = 0L;

        var device = new IoTDevice(
                deviceId,
                plotId,
                name,
                type,
                effectiveDepth,
                effectiveTexture,
                effectiveMultiplier,
                DeviceStatus.ACTIVE,
                null,
                initialRevision
        );

        device.registerDomainEvent(new IoTDeviceRegisteredEvent(
                deviceId.deviceId(),
                plotId.plotId(),
                name.value(),
                type.name(),
                Instant.now()
        ));

        return device;
    }

    /**
     * Factory method used exclusively by the persistence layer to reconstitute an existing IoTDevice aggregate.
     *
     * @param snapshot the immutable snapshot of the device state
     * @return the reconstituted {@link IoTDevice} instance
     * @throws IllegalArgumentException if snapshot is null
     */
    public static IoTDevice reconstitute(IoTDeviceSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("device.snapshot.null");
        }
        return new IoTDevice(
                snapshot.id(),
                snapshot.plotId(),
                snapshot.name(),
                snapshot.type(),
                snapshot.depth(),
                snapshot.soilTextureType(),
                snapshot.calibrationMultiplier(),
                snapshot.status(),
                snapshot.lastReadingTimestamp(),
                snapshot.revision()
        );
    }

    /**
     * Calibrates sensor calculation coefficients, physical depth, and soil texture.
     * Enforces active operational status and optimistic locking revision if provided.
     * Dispatches an {@link IoTDeviceCalibratedEvent}.
     *
     * @param newDepth         the updated installation depth (for soil probes, or null to keep current)
     * @param newTexture       the updated soil textural type (for soil probes, or null to keep current)
     * @param newMultiplier    the updated calibration multiplier
     * @param expectedRevision the expected optimistic concurrency revision (optional, null to bypass check)
     * @throws IllegalStateException if device is not in ACTIVE status
     * @throws DeviceRevisionMismatchException if expectedRevision does not match aggregate revision
     */
    public void calibrate(
            SensorDepth newDepth,
            SoilTextureType newTexture,
            CalibrationMultiplier newMultiplier,
            Long expectedRevision
    ) {
        if (this.status != DeviceStatus.ACTIVE) {
            throw new IllegalStateException("device.status.not_active");
        }
        if (expectedRevision != null && (this.revision == null || !this.revision.equals(expectedRevision))) {
            long currentRev = this.revision == null ? 0L : this.revision;
            throw new DeviceRevisionMismatchException(this.id, currentRev, expectedRevision);
        }
        if (newMultiplier == null) {
            throw new IllegalArgumentException("device.calibration.null");
        }

        if (this.type == DeviceType.SOIL_PROBE) {
            if (newDepth != null && newDepth.isPresent()) {
                this.depth = newDepth;
            }
            if (newTexture != null && newTexture != SoilTextureType.NOT_APPLICABLE) {
                this.soilTextureType = newTexture;
            }
        }
        this.calibrationMultiplier = newMultiplier;
        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        registerDomainEvent(new IoTDeviceCalibratedEvent(
                this.id.deviceId(),
                this.plotId.plotId(),
                this.calibrationMultiplier.value(),
                this.soilTextureType.name(),
                this.depth.depthCm(),
                this.revision,
                Instant.now()
        ));
    }

    /**
     * Backward-compatible overload for calibrating without revision check.
     *
     * @param newDepth      the updated installation depth
     * @param newTexture    the updated soil textural type
     * @param newMultiplier the updated calibration multiplier
     */
    public void calibrate(
            SensorDepth newDepth,
            SoilTextureType newTexture,
            CalibrationMultiplier newMultiplier
    ) {
        calibrate(newDepth, newTexture, newMultiplier, null);
    }

    /**
     * Calibrates empirical offset multiplier and soil parameters (semantic alias for calibrate).
     *
     * @param newMultiplier    the updated calibration multiplier
     * @param newTexture       the updated soil texture type
     * @param newDepth         the updated installation depth
     * @param expectedRevision the expected optimistic concurrency revision
     */
    public void calibrateOffset(
            CalibrationMultiplier newMultiplier,
            SoilTextureType newTexture,
            SensorDepth newDepth,
            Long expectedRevision
    ) {
        calibrate(newDepth, newTexture, newMultiplier, expectedRevision);
    }

    /**
     * Updates the descriptive name of the sensor node.
     *
     * @param newName the new descriptive name
     */
    public void rename(DeviceName newName) {
        if (newName == null) {
            throw new IllegalArgumentException("device.name.blank");
        }
        this.name = newName;
        this.revision = (this.revision == null ? 0L : this.revision) + 1L;
    }

    /**
     * Unlinks and logically deactivates this sensor device from the active plot inventory.
     * Enforces active operational status and optimistic locking revision if provided.
     * Dispatches an {@link IoTDeviceUnlinkedEvent}.
     *
     * @param expectedRevision the expected optimistic concurrency revision (optional, null to bypass check)
     * @throws IllegalStateException if device is already UNLINKED
     * @throws DeviceRevisionMismatchException if expectedRevision does not match aggregate revision
     */
    public void unlink(Long expectedRevision) {
        if (this.status == DeviceStatus.UNLINKED) {
            throw new IllegalStateException("device.already_unlinked");
        }
        if (expectedRevision != null && (this.revision == null || !this.revision.equals(expectedRevision))) {
            long currentRev = this.revision == null ? 0L : this.revision;
            throw new DeviceRevisionMismatchException(this.id, currentRev, expectedRevision);
        }
        this.status = DeviceStatus.UNLINKED;
        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        registerDomainEvent(new IoTDeviceUnlinkedEvent(
                this.id.deviceId(),
                this.plotId.plotId(),
                this.revision,
                Instant.now()
        ));
    }

    /**
     * Backward-compatible overload for unlinking without revision check.
     */
    public void unlink() {
        unlink(null);
    }

    /**
     * Returns an immutable snapshot capturing the current state of this aggregate.
     *
     * @return an {@link IoTDeviceSnapshot} instance
     */
    public IoTDeviceSnapshot snapshot() {
        return new IoTDeviceSnapshot(
                id,
                plotId,
                name,
                type,
                depth,
                soilTextureType,
                calibrationMultiplier,
                status,
                lastReadingTimestamp,
                revision
        );
    }
}
