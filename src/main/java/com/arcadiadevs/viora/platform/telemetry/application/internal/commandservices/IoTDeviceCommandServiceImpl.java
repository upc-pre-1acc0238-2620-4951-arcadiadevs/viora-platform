package com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.IoTDeviceCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.domain.exceptions.DeviceRevisionMismatchException;
import com.arcadiadevs.viora.platform.telemetry.domain.exceptions.DuplicateDeviceNameException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CalibrateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.DeactivateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation orchestrating IoT device mutation use cases.
 */
@Service
@Transactional
public class IoTDeviceCommandServiceImpl implements IoTDeviceCommandService {

    private final IoTDeviceRepository ioTDeviceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the IoTDeviceCommandServiceImpl with required dependencies.
     *
     * @param ioTDeviceRepository    the domain device repository port
     * @param eventPublisher         the Spring application event publisher
     * @param externalOrchardService the outbound ACL service for orchard verifications
     */
    public IoTDeviceCommandServiceImpl(
            IoTDeviceRepository ioTDeviceRepository,
            ApplicationEventPublisher eventPublisher,
            ExternalOrchardService externalOrchardService
    ) {
        if (ioTDeviceRepository == null) {
            throw new IllegalArgumentException("device.repository.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("device.event_publisher.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        this.ioTDeviceRepository = ioTDeviceRepository;
        this.eventPublisher = eventPublisher;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<String, ApplicationError> handle(RegisterIoTDeviceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command.null");
        }
        try {
            var plotId = new PlotId(command.plotId());

            if (!externalOrchardService.existsActivePlot(plotId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var deviceName = new DeviceName(command.name());

            if (ioTDeviceRepository.existsByNameAndPlotId(deviceName, plotId)) {
                return Result.failure(ApplicationError.conflict("device", "device.name.duplicate"));
            }

            var type = DeviceType.from(command.type());
            var depth = (command.depthCm() != null)
                    ? SensorDepth.of(command.depthCm())
                    : SensorDepth.none();
            var soilTexture = (command.soilTextureType() != null && !command.soilTextureType().isBlank())
                    ? SoilTextureType.from(command.soilTextureType())
                    : SoilTextureType.NOT_APPLICABLE;
            var multiplier = (command.calibrationMultiplier() != null)
                    ? new CalibrationMultiplier(command.calibrationMultiplier())
                    : CalibrationMultiplier.defaultMultiplier();

            var device = IoTDevice.register(
                    plotId,
                    deviceName,
                    type,
                    depth,
                    soilTexture,
                    multiplier
            );

            var savedDevice = ioTDeviceRepository.save(device);

            for (var event : device.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            device.clearDomainEvents();

            return Result.success(savedDevice.snapshot().id().deviceId());
        } catch (DuplicateDeviceNameException ex) {
            return Result.failure(ApplicationError.conflict("device", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("device-registration", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(CalibrateIoTDeviceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command.null");
        }
        try {
            var plotId = new PlotId(command.plotId());
            var deviceId = new DeviceId(command.deviceId());

            var deviceOpt = ioTDeviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("IoTDevice", command.deviceId()));
            }

            var device = deviceOpt.get();
            if (!device.snapshot().plotId().equals(plotId)) {
                return Result.failure(ApplicationError.conflict("device", "device.plot_id.mismatch"));
            }

            var multiplier = new CalibrationMultiplier(command.calibrationMultiplier());
            var texture = (command.soilTextureType() != null && !command.soilTextureType().isBlank())
                    ? SoilTextureType.from(command.soilTextureType())
                    : null;
            var depth = (command.depthCm() != null)
                    ? SensorDepth.of(command.depthCm())
                    : null;

            device.calibrateOffset(multiplier, texture, depth, command.expectedRevision());

            var savedDevice = ioTDeviceRepository.save(device);

            for (var event : device.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            device.clearDomainEvents();

            return Result.success(savedDevice.snapshot().id().deviceId());
        } catch (DeviceRevisionMismatchException ex) {
            return Result.failure(ApplicationError.preconditionFailed("device", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return Result.failure(ApplicationError.businessRuleViolation("DeviceNotActive", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("device-calibration", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(DeactivateIoTDeviceCommand command) {
        try {
            var plotId = new PlotId(command.plotId());
            var deviceId = new DeviceId(command.deviceId());

            var deviceOpt = ioTDeviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("IoTDevice", command.deviceId()));
            }

            var device = deviceOpt.get();
            if (!device.snapshot().plotId().equals(plotId)) {
                return Result.failure(ApplicationError.conflict("device", "device.plot_id.mismatch"));
            }

            device.unlink(command.expectedRevision());

            var savedDevice = ioTDeviceRepository.save(device);

            for (var event : device.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            device.clearDomainEvents();

            return Result.success(savedDevice.snapshot().id().deviceId());
        } catch (DeviceRevisionMismatchException ex) {
            return Result.failure(ApplicationError.preconditionFailed("device", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return Result.failure(ApplicationError.conflict("device", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("device-deactivation", ex.getMessage()));
        }
    }
}
