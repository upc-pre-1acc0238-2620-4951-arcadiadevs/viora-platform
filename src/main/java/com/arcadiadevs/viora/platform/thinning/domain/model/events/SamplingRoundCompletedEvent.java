package com.arcadiadevs.viora.platform.thinning.domain.model.events;

import java.time.Instant;

/**
 * Domain Event emitted when cumulative field samplings achieve representative statistical coverage (minimum 5 trees evaluated).
 *
 * @param prescriptionId the identifier of the prescription aggregate
 * @param plotId         the referenced plot identifier
 * @param evaluatedTrees the total count of representative unique trees evaluated
 * @param occurredOn     the timestamp when the event occurred
 */
public record SamplingRoundCompletedEvent(
        String prescriptionId,
        String plotId,
        Integer evaluatedTrees,
        Instant occurredOn
) {

    /**
     * Compact constructor validating event invariants.
     */
    public SamplingRoundCompletedEvent {
        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("thinning.event.prescription_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("thinning.event.plot_id.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("thinning.event.occurred_on.null");
        }
    }
}
