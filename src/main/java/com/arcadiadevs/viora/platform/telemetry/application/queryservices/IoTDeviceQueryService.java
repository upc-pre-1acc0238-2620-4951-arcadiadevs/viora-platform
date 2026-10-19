package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDeviceByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDevicesByPlotIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application query service port for querying virtual IoT devices.
 */
public interface IoTDeviceQueryService {

    /**
     * Retrieves all IoT devices bound to the target plot identifier.
     *
     * @param query the query containing the plot identifier
     * @return list of {@link IoTDevice} aggregate roots bound to the plot
     */
    List<IoTDevice> handle(GetIoTDevicesByPlotIdQuery query);

    /**
     * Retrieves one IoT device by its identifier.
     *
     * @param query the query carrying the device identifier
     * @return the device, or empty when it does not exist
     */
    Optional<IoTDevice> handle(GetIoTDeviceByIdQuery query);
}
