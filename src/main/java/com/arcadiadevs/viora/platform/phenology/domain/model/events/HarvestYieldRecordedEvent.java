package com.arcadiadevs.viora.platform.phenology.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an annual harvest yield entry is registered in the phenology history of a plot.
 *
 * @param entryId      the unique identifier of the harvest record
 * @param plotId       the identifier of the monitored plot
 * @param campaignYear the campaign year
 * @param totalYieldKg the total volume harvested in kilograms
 * @param occurredOn   the exact timestamp when the harvest was recorded
 */
public record HarvestYieldRecordedEvent(
        String entryId,
        String plotId,
        Integer campaignYear,
        Double totalYieldKg,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null event invariants.
     *
     * @throws IllegalArgumentException if mandatory fields are null or blank
     */
    public HarvestYieldRecordedEvent {
        if (entryId == null || entryId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.entry_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.plot_id.null");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (totalYieldKg == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("phenology.event.occurred_on.null");
        }
    }
}
