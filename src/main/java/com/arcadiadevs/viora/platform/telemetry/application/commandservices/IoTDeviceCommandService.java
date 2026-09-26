package com.arcadiadevs.viora.platform.telemetry.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;

/**
 * Application service interface orchestrating mutating commands for IoT devices in the telemetry context.
 */
public interface IoTDeviceCommandService {

    /**
     * Handles registering and binding a new virtual IoT sensor node to an orchard plot.
     *
     * @param command command containing device parameters and plot reference
     * @return the created device identifier UUID string or an application error
     * @see RegisterIoTDeviceCommand
     */
    Result<String, ApplicationError> handle(RegisterIoTDeviceCommand command);
}
