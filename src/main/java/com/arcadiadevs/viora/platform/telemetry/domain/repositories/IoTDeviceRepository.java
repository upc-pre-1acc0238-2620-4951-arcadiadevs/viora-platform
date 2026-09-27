package com.arcadiadevs.viora.platform.telemetry.domain.repositories;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceName;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for managing the persistence lifecycle of {@link IoTDevice} aggregate roots.
 * Pure Java interface decoupled from Spring Data or Hibernate annotations.
 */
public interface IoTDeviceRepository {

    /**
     * Persists an IoT device aggregate root.
     *
     * @param device the device aggregate to save
     * @return the saved device aggregate root
     */
    IoTDevice save(IoTDevice device);

    /**
     * Finds an IoT device by its unique domain identifier.
     *
     * @param id the device identifier value object
     * @return an {@link Optional} containing the found device, or empty if not found
     */
    Optional<IoTDevice> findById(DeviceId id);

    /**
     * Checks if a device with the same name already exists within the target plot.
     *
     * @param name   the device name to check
     * @param plotId the plot identifier
     * @return true if a device with the given name exists in the plot, false otherwise
     */
    boolean existsByNameAndPlotId(DeviceName name, PlotId plotId);

    /**
     * Finds all IoT devices installed within a given plot.
     *
     * @param plotId the plot identifier
     * @return list of devices installed in the plot
     */
    List<IoTDevice> findAllByPlotId(PlotId plotId);

    /**
     * Finds all active IoT devices installed within a given plot.
     *
     * @param plotId the plot identifier
     * @return list of active devices installed in the plot
     */
    List<IoTDevice> findActiveByPlotId(PlotId plotId);
}
