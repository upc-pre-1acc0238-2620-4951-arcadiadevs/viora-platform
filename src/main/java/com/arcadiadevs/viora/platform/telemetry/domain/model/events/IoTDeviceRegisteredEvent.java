package com.arcadiadevs.viora.platform.telemetry.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when a new IoT sensor device is registered and bound to an orchard plot.
 *
 * @param deviceId   the unique identifier of the registered device
 * @param plotId     the identifier of the plot being instrumented
 * @param name       the descriptive name of the device
 * @param type       the functional classification string of the device
 * @param occurredOn the exact timestamp when the registration event occurred
 */
public record IoTDeviceRegisteredEvent(
        String deviceId,
        String plotId,
        String name,
        String type,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event invariants.
     *
     * @throws IllegalArgumentException if any mandatory event field is null or blank
     */
    public IoTDeviceRegisteredEvent {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.event.device_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("device.event.plot_id.null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("device.event.name.null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("device.event.type.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("device.event.occurred_on.null");
        }
    }
}
