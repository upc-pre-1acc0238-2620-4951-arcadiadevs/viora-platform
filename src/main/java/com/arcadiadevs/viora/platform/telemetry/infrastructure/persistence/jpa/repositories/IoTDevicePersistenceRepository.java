package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceName;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.IoTDevicePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link IoTDevicePersistenceEntity}.
 */
@Repository
public interface IoTDevicePersistenceRepository extends JpaRepository<IoTDevicePersistenceEntity, UUID> {

    /**
     * Checks if a device entity exists with the specified name in the given plot.
     *
     * @param name   the device name value object
     * @param plotId the plot identifier value object
     * @return true if an entity exists, false otherwise
     */
    boolean existsByNameAndPlotId(DeviceName name, PlotId plotId);

    /**
     * Finds all device entities associated with a specific plot.
     *
     * @param plotId the plot identifier value object
     * @return list of persistence entities in the plot
     */
    List<IoTDevicePersistenceEntity> findAllByPlotId(PlotId plotId);

    /**
     * Finds all device entities associated with a specific plot matching an operational status.
     *
     * @param plotId the plot identifier value object
     * @param status the operational status
     * @return list of matching persistence entities
     */
    List<IoTDevicePersistenceEntity> findAllByPlotIdAndStatus(PlotId plotId, DeviceStatus status);
}
