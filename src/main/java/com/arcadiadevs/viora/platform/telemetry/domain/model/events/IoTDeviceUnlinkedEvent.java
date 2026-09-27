package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an IoT device is logically unlinked and deactivated from an orchard plot.
 *
 * @param deviceId   the unique identifier of the unlinked device
 * @param plotId     the identifier of the plot from which the device was unlinked
 * @param revision   the updated aggregate revision after unlinking
 * @param occurredOn the exact timestamp when the unlinking occurred
 */
public record IoTDeviceUnlinkedEvent(
        String deviceId,
        String plotId,
        Long revision,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event invariants.
     *
     * @throws IllegalArgumentException if mandatory fields are missing
     */
    public IoTDeviceUnlinkedEvent {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.event.device_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("device.event.plot_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("device.event.occurred_on.null");
        }
    }
}
