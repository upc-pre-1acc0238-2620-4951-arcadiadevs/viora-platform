package com.arcadiadevs.viora.platform.telemetry.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceName;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

/**
 * Domain exception thrown when registering a device with a name that already exists within the target plot.
 */
public class DuplicateDeviceNameException extends ResourceConflictException {

    /**
     * Constructs a DuplicateDeviceNameException with context details.
     *
     * @param name   the duplicate device name
     * @param plotId the plot identifier value object
     */
    public DuplicateDeviceNameException(DeviceName name, PlotId plotId) {
        super("device.name.duplicate");
    }
}
