package com.arcadiadevs.viora.platform.telemetry.domain.model;

import com.arcadiadevs.viora.platform.telemetry.domain.exceptions.DeviceRevisionMismatchException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceCalibratedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceRegisteredEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IoTDevice Aggregate Root and Value Objects Domain Unit Tests")
class IoTDeviceTest {

    private final PlotId plotId = new PlotId();

    @Test
    @DisplayName("Should successfully register a microclimate station with default multiplier and event")
    void shouldSuccessfullyRegisterMicroclimateDevice() {
        var name = new DeviceName("Estacion Metereologica Central");
        var type = DeviceType.MICROCLIMATE;

        var device = IoTDevice.register(
                plotId,
                name,
                type,
                SensorDepth.none(),
                SoilTextureType.NOT_APPLICABLE,
                null
        );

        assertThat(device).isNotNull();
        var snapshot = device.snapshot();
        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.id().deviceId()).isNotBlank();
        assertThat(snapshot.plotId().plotId()).isEqualTo(plotId.plotId());
        assertThat(snapshot.name().value()).isEqualTo("Estacion Metereologica Central");
        assertThat(snapshot.type()).isEqualTo(DeviceType.MICROCLIMATE);
        assertThat(snapshot.depth().isPresent()).isFalse();
        assertThat(snapshot.soilTextureType()).isEqualTo(SoilTextureType.NOT_APPLICABLE);
        assertThat(snapshot.calibrationMultiplier().value()).isEqualTo(1.0);
        assertThat(snapshot.status()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(snapshot.revision()).isEqualTo(0L);

        assertThat(device.domainEvents()).hasSize(1);
        var event = (IoTDeviceRegisteredEvent) device.domainEvents().iterator().next();
        assertThat(event.deviceId()).isEqualTo(snapshot.id().deviceId());
        assertThat(event.plotId()).isEqualTo(plotId.plotId());
        assertThat(event.name()).isEqualTo("Estacion Metereologica Central");
        assertThat(event.type()).isEqualTo("MICROCLIMATE");
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully register a soil probe with specified depth and texture")
    void shouldSuccessfullyRegisterSoilProbeDevice() {
        var name = new DeviceName("Sonda Edafica Sector Norte");
        var type = DeviceType.SOIL_PROBE;
        var depth = SensorDepth.of(30);
        var texture = SoilTextureType.SANDY_LOAM;
        var multiplier = new CalibrationMultiplier(1.15);

        var device = IoTDevice.register(plotId, name, type, depth, texture, multiplier);

        assertThat(device).isNotNull();
        var snapshot = device.snapshot();
        assertThat(snapshot.type()).isEqualTo(DeviceType.SOIL_PROBE);
        assertThat(snapshot.depth().depthCm()).isEqualTo(30);
        assertThat(snapshot.soilTextureType()).isEqualTo(SoilTextureType.SANDY_LOAM);
        assertThat(snapshot.calibrationMultiplier().value()).isEqualTo(1.15);
        assertThat(snapshot.status()).isEqualTo(DeviceStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering soil probe without depth")
    void shouldThrowWhenRegisteringSoilProbeWithoutDepth() {
        var name = new DeviceName("Sonda Invalida");
        var type = DeviceType.SOIL_PROBE;

        assertThatThrownBy(() -> IoTDevice.register(plotId, name, type, SensorDepth.none(), SoilTextureType.LOAM, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.soil_probe.depth_required");
    }

    @Test
    @DisplayName("Should validate DeviceId correctly for null, blank, and invalid UUID formats")
    void shouldValidateDeviceIdCorrectly() {
        var generatedId = new DeviceId();
        assertThat(generatedId.deviceId()).isNotBlank();
        assertThat(UUID.fromString(generatedId.deviceId())).isNotNull();

        assertThatThrownBy(() -> new DeviceId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.id.null_or_empty");

        assertThatThrownBy(() -> new DeviceId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.id.null_or_empty");

        assertThatThrownBy(() -> new DeviceId("invalid-uuid-format"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.id.invalid_uuid");
    }

    @Test
    @DisplayName("Should validate DeviceName length and blank invariants")
    void shouldValidateDeviceNameCorrectly() {
        var valid = new DeviceName("Sensor 01");
        assertThat(valid.value()).isEqualTo("Sensor 01");

        assertThatThrownBy(() -> new DeviceName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.name.blank");

        assertThatThrownBy(() -> new DeviceName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.name.blank");

        assertThatThrownBy(() -> new DeviceName("A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.name.invalid_length");

        assertThatThrownBy(() -> new DeviceName("A".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.name.invalid_length");
    }

    @Test
    @DisplayName("Should validate CalibrationMultiplier range [0.50, 2.00]")
    void shouldValidateCalibrationMultiplierCorrectly() {
        var valid = new CalibrationMultiplier(1.5);
        assertThat(valid.value()).isEqualTo(1.5);

        assertThatThrownBy(() -> new CalibrationMultiplier(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.calibration.null");

        assertThatThrownBy(() -> new CalibrationMultiplier(0.49))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.calibration.out_of_range");

        assertThatThrownBy(() -> new CalibrationMultiplier(2.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.calibration.out_of_range");
    }

    @Test
    @DisplayName("Should validate SensorDepth bounds")
    void shouldValidateSensorDepthCorrectly() {
        var valid = SensorDepth.of(60);
        assertThat(valid.depthCm()).isEqualTo(60);
        assertThat(valid.isPresent()).isTrue();

        assertThatThrownBy(() -> SensorDepth.of(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.depth.invalid");

        assertThatThrownBy(() -> SensorDepth.of(-10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.depth.invalid");

        assertThatThrownBy(() -> SensorDepth.of(201))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.depth.invalid");
    }

    @Test
    @DisplayName("Should calibrate and unlink device updating status and revision")
    void shouldMutateDeviceCorrectly() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Sur"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                CalibrationMultiplier.defaultMultiplier()
        );

        device.calibrate(SensorDepth.of(60), SoilTextureType.CLAY_LOAM, new CalibrationMultiplier(1.2));
        var calibrated = device.snapshot();
        assertThat(calibrated.depth().depthCm()).isEqualTo(60);
        assertThat(calibrated.soilTextureType()).isEqualTo(SoilTextureType.CLAY_LOAM);
        assertThat(calibrated.calibrationMultiplier().value()).isEqualTo(1.2);
        assertThat(calibrated.revision()).isEqualTo(1L);

        device.rename(new DeviceName("Sonda Sur Recalibrada"));
        assertThat(device.snapshot().name().value()).isEqualTo("Sonda Sur Recalibrada");
        assertThat(device.snapshot().revision()).isEqualTo(2L);

        device.unlink();
        assertThat(device.snapshot().status()).isEqualTo(DeviceStatus.UNLINKED);
        assertThat(device.snapshot().revision()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should reconstitute device from snapshot preserving all attributes")
    void shouldReconstituteFromSnapshotCorrectly() {
        var original = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Este"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(45),
                SoilTextureType.CLAY,
                new CalibrationMultiplier(1.1)
        );

        var snapshot = original.snapshot();
        var reconstituted = IoTDevice.reconstitute(snapshot);

        assertThat(reconstituted.snapshot()).isEqualTo(snapshot);
    }

    @Test
    @DisplayName("Should successfully calibrate soil probe offset with event and revision increment")
    void shouldSuccessfullyCalibrateSoilProbeOffsetWithEventAndRevisionIncrement() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Calibrable"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );
        device.clearDomainEvents();

        device.calibrateOffset(
                new CalibrationMultiplier(1.25),
                SoilTextureType.CLAY_LOAM,
                SensorDepth.of(50),
                0L
        );

        var snapshot = device.snapshot();
        assertThat(snapshot.calibrationMultiplier().value()).isEqualTo(1.25);
        assertThat(snapshot.soilTextureType()).isEqualTo(SoilTextureType.CLAY_LOAM);
        assertThat(snapshot.depth().depthCm()).isEqualTo(50);
        assertThat(snapshot.revision()).isEqualTo(1L);

        assertThat(device.domainEvents()).hasSize(1);
        var event = (IoTDeviceCalibratedEvent) device.domainEvents().iterator().next();
        assertThat(event.deviceId()).isEqualTo(snapshot.id().deviceId());
        assertThat(event.plotId()).isEqualTo(plotId.plotId());
        assertThat(event.calibrationMultiplier()).isEqualTo(1.25);
        assertThat(event.soilTextureType()).isEqualTo("CLAY_LOAM");
        assertThat(event.depthCm()).isEqualTo(50);
        assertThat(event.revision()).isEqualTo(1L);
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("Should calibrate microclimate multiplier without altering depth or soil texture")
    void shouldCalibrateMicroclimateWithoutAlteringDepthOrSoilTexture() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Estacion Metereologica"),
                DeviceType.MICROCLIMATE,
                SensorDepth.none(),
                SoilTextureType.NOT_APPLICABLE,
                null
        );

        device.calibrateOffset(
                new CalibrationMultiplier(1.35),
                SoilTextureType.SANDY,
                SensorDepth.of(40),
                0L
        );

        var snapshot = device.snapshot();
        assertThat(snapshot.calibrationMultiplier().value()).isEqualTo(1.35);
        assertThat(snapshot.soilTextureType()).isEqualTo(SoilTextureType.NOT_APPLICABLE);
        assertThat(snapshot.depth().isPresent()).isFalse();
        assertThat(snapshot.revision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw DeviceRevisionMismatchException when expected revision does not match")
    void shouldThrowDeviceRevisionMismatchExceptionWhenRevisionDiffers() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Revision"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        assertThatThrownBy(() -> device.calibrateOffset(
                new CalibrationMultiplier(1.2),
                SoilTextureType.SANDY,
                SensorDepth.of(30),
                5L
        )).isInstanceOf(DeviceRevisionMismatchException.class);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when calibrating inactive device")
    void shouldThrowIllegalStateExceptionWhenCalibratingInactiveDevice() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Inactiva"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );
        device.unlink();

        assertThatThrownBy(() -> device.calibrateOffset(
                new CalibrationMultiplier(1.2),
                SoilTextureType.SANDY,
                SensorDepth.of(30),
                null
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("device.status.not_active");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when calibration multiplier is null")
    void shouldThrowIllegalArgumentExceptionWhenCalibrationMultiplierIsNull() {
        var device = IoTDevice.register(
                plotId,
                new DeviceName("Sonda Multiplier Null"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        assertThatThrownBy(() -> device.calibrateOffset(
                null,
                SoilTextureType.SANDY,
                SensorDepth.of(30),
                0L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.calibration.null");
    }
}
