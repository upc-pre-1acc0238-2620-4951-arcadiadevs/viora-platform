package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable domain value object representing a thinning lifecycle milestone event for the agronomic logbook.
 *
 * @param id                  unique event identifier
 * @param eventType           type of milestone event (e.g. SAMPLING_COMPLETED, THINNING_EXECUTED)
 * @param prescriptionId      thinning prescription identifier
 * @param confirmationId      execution confirmation identifier (null for non-execution events)
 * @param plotId              plot identifier
 * @param plotName            human-readable plot name
 * @param campaignYear        agricultural campaign year
 * @param occurredAt          timestamp when event occurred
 * @param evaluatedTreesCount evaluated trees count (sampling events only)
 * @param totalShootsCount    total shoots counted (sampling events only)
 * @param totalFruitsCount    total fruits counted (sampling events only)
 * @param meanFruitsPerShoot  mean fruits per shoot (sampling events only)
 * @param isRepresentative    representativeness flag (sampling events only)
 * @param removalPercentage   percentage of fruits removed (execution events only)
 * @param removedKg           biomass removed in kilograms (execution events only)
 * @param executedDate        execution date (execution events only)
 * @param laborCrewSize       worker crew size (execution events only)
 * @param timeliness          timeliness status (execution events only)
 */
public record ThinningEvent(
        String id,
        String eventType,
        String prescriptionId,
        String confirmationId,
        PlotId plotId,
        String plotName,
        CampaignYear campaignYear,
        Instant occurredAt,
        Integer evaluatedTreesCount,
        Integer totalShootsCount,
        Integer totalFruitsCount,
        Double meanFruitsPerShoot,
        Boolean isRepresentative,
        Double removalPercentage,
        Double removedKg,
        LocalDate executedDate,
        Integer laborCrewSize,
        String timeliness
) {

    /**
     * Compact constructor validating strictly non-null required identification fields.
     */
    public ThinningEvent {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("thinning.event.id.null_or_empty");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("thinning.event.type.null_or_empty");
        }
        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("thinning.prescription.id.null_or_empty");
        }
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("thinning.event.occurred_at.null");
        }
    }
}
