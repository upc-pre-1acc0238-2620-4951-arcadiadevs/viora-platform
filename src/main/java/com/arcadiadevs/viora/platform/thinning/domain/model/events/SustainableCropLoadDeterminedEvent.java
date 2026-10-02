package com.arcadiadevs.viora.platform.thinning.domain.model.events;

import java.time.Instant;

/**
 * Domain event emitted when a sustainable crop-load prescription is established.
 *
 * @param prescriptionId   prescription aggregate identifier
 * @param plotId            target plot identifier
 * @param removalPercentage recommended fruit-removal percentage
 * @param occurredOn        event timestamp
 */
public record SustainableCropLoadDeterminedEvent(
        String prescriptionId,
        String plotId,
        Double removalPercentage,
        Instant occurredOn
) {

    /**
     * Compact constructor validating event invariants.
     */
    public SustainableCropLoadDeterminedEvent {
        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("thinning.event.prescription_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("thinning.event.plot_id.null");
        }
        if (removalPercentage == null || !Double.isFinite(removalPercentage)
                || removalPercentage < 0.0 || removalPercentage > 100.0) {
            throw new IllegalArgumentException("thinning.event.removal_percentage.invalid");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("thinning.event.occurred_on.null");
        }
    }
}
