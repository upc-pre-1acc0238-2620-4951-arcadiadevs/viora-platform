package com.arcadiadevs.viora.platform.telemetry.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;

/**
 * Domain exception thrown when an IoT sensor device aggregate cannot be found.
 */
public class DeviceNotFoundException extends ResourceNotFoundException {

    /**
     * Constructs the exception using a typed {@link DeviceId}.
     *
     * @param deviceId the device identifier that was not found
     */
    public DeviceNotFoundException(DeviceId deviceId) {
        super("IoTDevice", deviceId != null ? deviceId.deviceId() : "null");
    }

    /**
     * Constructs the exception with an explicit i18n message key.
     *
     * @param message the exception message or i18n key
     */
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
