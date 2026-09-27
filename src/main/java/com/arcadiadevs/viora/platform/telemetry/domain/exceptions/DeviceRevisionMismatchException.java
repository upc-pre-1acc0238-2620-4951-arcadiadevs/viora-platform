package com.arcadiadevs.viora.platform.telemetry.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;

/**
 * Exception thrown when an optimistic concurrency revision mismatch occurs on an {@link DeviceId}.
 */
public class DeviceRevisionMismatchException extends BusinessRuleException {

    private final DeviceId deviceId;
    private final long currentRevision;
    private final long expectedRevision;

    /**
     * Constructs a DeviceRevisionMismatchException with device details and revisions.
     *
     * @param deviceId         the identifier of the device being mutated
     * @param currentRevision  the actual current aggregate revision
     * @param expectedRevision the expected revision supplied by the caller
     */
    public DeviceRevisionMismatchException(DeviceId deviceId, long currentRevision, long expectedRevision) {
        super("device.revision.mismatch");
        this.deviceId = deviceId;
        this.currentRevision = currentRevision;
        this.expectedRevision = expectedRevision;
    }

    /**
     * Returns the identifier of the device whose revision mismatched.
     *
     * @return the device identifier value object
     */
    public DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the actual current revision of the device aggregate in domain.
     *
     * @return the current revision number
     */
    public long getCurrentRevision() {
        return currentRevision;
    }

    /**
     * Returns the revision expected by the client caller.
     *
     * @return the expected revision number
     */
    public long getExpectedRevision() {
        return expectedRevision;
    }
}
