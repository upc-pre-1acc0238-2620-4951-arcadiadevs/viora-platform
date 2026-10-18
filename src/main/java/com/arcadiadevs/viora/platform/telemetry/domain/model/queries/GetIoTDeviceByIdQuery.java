package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;

/**
 * Domain query for retrieving one IoT device by its identifier.
 *
 * @param deviceId the device identifier
 */
public record GetIoTDeviceByIdQuery(DeviceId deviceId) {
    public GetIoTDeviceByIdQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("device.id.null_or_empty");
        }
    }

    /**
     * Creates the query from a raw device identifier.
     *
     * @param rawDeviceId the device UUID string
     */
    public GetIoTDeviceByIdQuery(String rawDeviceId) {
        this(new DeviceId(rawDeviceId));
    }
}
