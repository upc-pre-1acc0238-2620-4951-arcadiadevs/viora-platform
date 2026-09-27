package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices.IoTDeviceCommandServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CalibrateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceCalibratedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.IoTDeviceRegisteredEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IoTDeviceCommandService Application Unit Tests")
class IoTDeviceCommandServiceTest {

    @Mock
    private IoTDeviceRepository ioTDeviceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private IoTDeviceCommandServiceImpl commandService;
    private final UUID plotId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commandService = new IoTDeviceCommandServiceImpl(ioTDeviceRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully register device when name is unique and parameters are valid")
    void shouldSuccessfullyRegisterDevice() {
        var command = new RegisterIoTDeviceCommand(
                plotId.toString(),
                "Estacion Principal",
                "MICROCLIMATE",
                null,
                null,
                1.0
        );

        when(ioTDeviceRepository.existsByNameAndPlotId(any(DeviceName.class), eq(new PlotId(plotId.toString()))))
                .thenReturn(false);
        when(ioTDeviceRepository.save(any(IoTDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = commandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();
        assertThat(result.success().get()).isNotBlank();

        verify(ioTDeviceRepository).save(any(IoTDevice.class));
        verify(eventPublisher).publishEvent(any(IoTDeviceRegisteredEvent.class));
    }

    @Test
    @DisplayName("Should return conflict error when device name already exists in the plot")
    void shouldReturnConflictWhenNameExists() {
        var command = new RegisterIoTDeviceCommand(
                plotId.toString(),
                "Estacion Repetida",
                "MICROCLIMATE",
                null,
                null,
                1.0
        );

        when(ioTDeviceRepository.existsByNameAndPlotId(any(DeviceName.class), eq(new PlotId(plotId.toString()))))
                .thenReturn(true);

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("DEVICE_CONFLICT");
        assertThat(result.failure().get().details()).isEqualTo("device.name.duplicate");

        verify(ioTDeviceRepository, never()).save(any(IoTDevice.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should return validation error when soil probe has no depth")
    void shouldReturnValidationErrorWhenSoilProbeMissingDepth() {
        var command = new RegisterIoTDeviceCommand(
                plotId.toString(),
                "Sonda Sin Profundidad",
                "SOIL_PROBE",
                null,
                "LOAM",
                1.0
        );

        when(ioTDeviceRepository.existsByNameAndPlotId(any(DeviceName.class), eq(new PlotId(plotId.toString()))))
                .thenReturn(false);

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.failure().get().details()).isEqualTo("device.soil_probe.depth_required");

        verify(ioTDeviceRepository, never()).save(any(IoTDevice.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should successfully calibrate device when plot matches and parameters are valid")
    void shouldSuccessfullyCalibrateDevice() {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda a Calibrar"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceRepository.findById(eq(device.snapshot().id())))
                .thenReturn(Optional.of(device));
        when(ioTDeviceRepository.save(any(IoTDevice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new CalibrateIoTDeviceCommand(
                plotId.toString(),
                device.snapshot().id().deviceId(),
                1.25,
                "CLAY_LOAM",
                45,
                0L
        );

        var result = commandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).contains(device.snapshot().id().deviceId());

        verify(ioTDeviceRepository).save(any(IoTDevice.class));
        verify(eventPublisher).publishEvent(any(IoTDeviceCalibratedEvent.class));
    }

    @Test
    @DisplayName("Should return not found error when calibrating non-existent device")
    void shouldReturnNotFoundWhenCalibratingNonExistentDevice() {
        var nonExistentId = UUID.randomUUID().toString();
        when(ioTDeviceRepository.findById(eq(new DeviceId(nonExistentId))))
                .thenReturn(Optional.empty());

        var command = new CalibrateIoTDeviceCommand(
                plotId.toString(),
                nonExistentId,
                1.1,
                "SANDY",
                20,
                null
        );

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("IOTDEVICE_NOT_FOUND");
        verify(ioTDeviceRepository, never()).save(any(IoTDevice.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should return conflict error when device does not belong to specified plot")
    void shouldReturnConflictWhenPlotMismatch() {
        var otherPlotId = UUID.randomUUID();
        var device = IoTDevice.register(
                new PlotId(otherPlotId.toString()),
                new DeviceName("Sonda de Otro Lote"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceRepository.findById(eq(device.snapshot().id())))
                .thenReturn(Optional.of(device));

        var command = new CalibrateIoTDeviceCommand(
                plotId.toString(),
                device.snapshot().id().deviceId(),
                1.15,
                "LOAM",
                30,
                null
        );

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("DEVICE_CONFLICT");
        assertThat(result.failure().get().details()).isEqualTo("device.plot_id.mismatch");
        verify(ioTDeviceRepository, never()).save(any(IoTDevice.class));
    }

    @Test
    @DisplayName("Should return precondition failed when revision does not match expected")
    void shouldReturnPreconditionFailedWhenRevisionMismatch() {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda Revision"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );

        when(ioTDeviceRepository.findById(eq(device.snapshot().id())))
                .thenReturn(Optional.of(device));

        var command = new CalibrateIoTDeviceCommand(
                plotId.toString(),
                device.snapshot().id().deviceId(),
                1.2,
                "LOAM",
                30,
                99L
        );

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("DEVICE_PRECONDITION_FAILED");
        assertThat(result.failure().get().details()).isEqualTo("device.revision.mismatch");
    }

    @Test
    @DisplayName("Should return business rule violation when calibrating unlinked device")
    void shouldReturnBusinessRuleViolationWhenDeviceUnlinked() {
        var device = IoTDevice.register(
                new PlotId(plotId.toString()),
                new DeviceName("Sonda Desvinculada"),
                DeviceType.SOIL_PROBE,
                SensorDepth.of(30),
                SoilTextureType.LOAM,
                new CalibrationMultiplier(1.0)
        );
        device.unlink();

        when(ioTDeviceRepository.findById(eq(device.snapshot().id())))
                .thenReturn(Optional.of(device));

        var command = new CalibrateIoTDeviceCommand(
                plotId.toString(),
                device.snapshot().id().deviceId(),
                1.2,
                "LOAM",
                30,
                null
        );

        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().get().code()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(result.failure().get().details()).isEqualTo("device.status.not_active");
    }
}
