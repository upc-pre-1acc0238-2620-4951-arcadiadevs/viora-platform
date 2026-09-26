package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RegisterIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.RegisterIoTDeviceResource;

/**
 * Assembler converting a {@link RegisterIoTDeviceResource} payload to a {@link RegisterIoTDeviceCommand}.
 */
public final class RegisterIoTDeviceCommandFromResourceAssembler {

    private RegisterIoTDeviceCommandFromResourceAssembler() {
    }

    /**
     * Converts a REST request resource and path plotId into a domain command.
     *
     * @param plotId   the target plot identifier from path variable
     * @param resource the request payload resource
     * @return the corresponding {@link RegisterIoTDeviceCommand}
     */
    public static RegisterIoTDeviceCommand toCommandFromResource(String plotId, RegisterIoTDeviceResource resource) {
        return new RegisterIoTDeviceCommand(
                plotId,
                resource.name(),
                resource.type(),
                resource.depthCm(),
                resource.soilTextureType(),
                resource.calibrationMultiplier()
        );
    }
}
