package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CalibrateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.CalibrateIoTDeviceResource;

/**
 * Assembler mapping an incoming {@link CalibrateIoTDeviceResource} payload to a {@link CalibrateIoTDeviceCommand}.
 */
public final class CalibrateIoTDeviceCommandFromResourceAssembler {

    private CalibrateIoTDeviceCommandFromResourceAssembler() {
    }

    /**
     * Converts route parameters and request body to a domain command.
     *
     * @param plotId           the target plot UUID string
     * @param deviceId         the target device UUID string
     * @param expectedRevision the optimistic concurrency revision (optional, nullable)
     * @param resource         the HTTP request body payload
     * @return the mapped domain command
     */
    public static CalibrateIoTDeviceCommand toCommandFromResource(
            String plotId,
            String deviceId,
            Long expectedRevision,
            CalibrateIoTDeviceResource resource
    ) {
        return new CalibrateIoTDeviceCommand(
                plotId,
                deviceId,
                resource.calibrationMultiplier(),
                resource.soilTextureType(),
                resource.depthCm(),
                expectedRevision
        );
    }
}
