package com.arcadiadevs.viora.platform.thinning.domain.model.events;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Published execution evidence for Harvest Settlement; contains no domain objects.
 * {@code prescribedRemovalPercentage} lets Settlement compare the actual removal with the prescription.
 */
public record ThinningExecutionConfirmedEvent(
        String eventId, String confirmationId, String prescriptionId, String plotId,
        Integer campaignYear, LocalDate executedDate, Double removalPercentage,
        Double removedKg, Integer laborCrewSize, String timeliness, Instant occurredOn,
        Double prescribedRemovalPercentage) {
}
