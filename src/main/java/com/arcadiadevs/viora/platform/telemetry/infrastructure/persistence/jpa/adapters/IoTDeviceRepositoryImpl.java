package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceName;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers.IoTDevicePersistenceAssembler;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.IoTDevicePersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.IoTDevicePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging the domain {@link IoTDeviceRepository} port with Spring Data JPA.
 */
@Repository
public class IoTDeviceRepositoryImpl implements IoTDeviceRepository {

    private final IoTDevicePersistenceRepository ioTDevicePersistenceRepository;

    /**
     * Constructs the adapter injecting the Spring Data persistence repository.
     *
     * @param ioTDevicePersistenceRepository the underlying Spring Data JPA repository
     */
    public IoTDeviceRepositoryImpl(IoTDevicePersistenceRepository ioTDevicePersistenceRepository) {
        this.ioTDevicePersistenceRepository = ioTDevicePersistenceRepository;
    }

    @Override
    public IoTDevice save(IoTDevice device) {
        var uuid = UUID.fromString(device.snapshot().id().deviceId());
        var existingOpt = ioTDevicePersistenceRepository.findById(uuid);

        IoTDevicePersistenceEntity entityToSave;
        if (existingOpt.isPresent()) {
            entityToSave = IoTDevicePersistenceAssembler.updateEntityFromDomain(existingOpt.get(), device);
        } else {
            entityToSave = IoTDevicePersistenceAssembler.toPersistenceFromDomain(device);
        }

        var savedEntity = ioTDevicePersistenceRepository.save(entityToSave);
        return IoTDevicePersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public Optional<IoTDevice> findById(DeviceId id) {
        return ioTDevicePersistenceRepository.findById(UUID.fromString(id.deviceId()))
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByNameAndPlotId(DeviceName name, PlotId plotId) {
        return ioTDevicePersistenceRepository.existsByNameAndPlotId(name, plotId);
    }

    @Override
    public List<IoTDevice> findAllByPlotId(PlotId plotId) {
        return ioTDevicePersistenceRepository.findAllByPlotId(plotId)
                .stream()
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<IoTDevice> findActiveByPlotId(PlotId plotId) {
        return ioTDevicePersistenceRepository.findAllByPlotIdAndStatus(plotId, DeviceStatus.ACTIVE)
                .stream()
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
