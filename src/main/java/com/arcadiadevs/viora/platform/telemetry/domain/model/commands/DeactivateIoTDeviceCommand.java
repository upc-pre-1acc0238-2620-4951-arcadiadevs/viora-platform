package com.arcadiadevs.viora.platform.telemetry.domain.model.commands;

/**
 * Domain command to unlink and logically deactivate an active IoT device from an orchard plot.
 * Follows strict Model A rules: validates non-null mandatory fields with i18n keys without business logic.
 *
 * @param plotId           the unique identifier of the plot instrumented
 * @param deviceId         the unique identifier of the device to unlink
 * @param expectedRevision the expected optimistic concurrency revision (optional, nullable)
 */
public record DeactivateIoTDeviceCommand(
        String plotId,
        String deviceId,
        Long expectedRevision
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory command attributes.
     *
     * @throws IllegalArgumentException if plotId or deviceId is null or invalid
     */
    public DeactivateIoTDeviceCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("device.id.null_or_empty");
        }
    }
}
