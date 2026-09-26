package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.IoTDeviceResource;

import java.util.List;

/**
 * Assembler converting an {@link IoTDevice} domain aggregate to an {@link IoTDeviceResource} representation.
 */
public final class IoTDeviceResourceFromEntityAssembler {

    private IoTDeviceResourceFromEntityAssembler() {
    }

    /**
     * Converts a domain aggregate into a public REST response resource.
     *
     * @param entity the domain aggregate to map
     * @return the corresponding {@link IoTDeviceResource}
     */
    public static IoTDeviceResource toResourceFromEntity(IoTDevice entity) {
        var snap = entity.snapshot();
        return new IoTDeviceResource(
                snap.id().deviceId(),
                snap.plotId().plotId(),
                snap.name().value(),
                snap.type().name(),
                snap.depth() != null ? snap.depth().depthCm() : null,
                snap.soilTextureType().name(),
                snap.calibrationMultiplier().value(),
                snap.status().name(),
                snap.lastReadingTimestamp(),
                snap.revision()
        );
    }

    /**
     * Converts a list of domain aggregates to a list of REST response resources.
     *
     * @param entities the list of domain aggregates
     * @return list of mapped {@link IoTDeviceResource} instances
     */
    public static List<IoTDeviceResource> toResourceList(List<IoTDevice> entities) {
        return entities.stream()
                .map(IoTDeviceResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
