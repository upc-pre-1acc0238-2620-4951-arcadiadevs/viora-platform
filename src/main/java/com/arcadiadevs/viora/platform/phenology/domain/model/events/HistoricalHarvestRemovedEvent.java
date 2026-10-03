package com.arcadiadevs.viora.platform.phenology.domain.model.events;

import java.time.Instant;

/**
 * Domain event emitted when a historical harvest record is removed from a plot's history and the
 * Hoblyn Biennial Bearing Index is recalculated over the remaining valid campaigns.
 *
 * @param entryId             the identifier of the removed harvest entry
 * @param plotId              the plot identifier
 * @param campaignYear        the campaign year of the removed entry
 * @param recalculatedBbi     the BBI over the remaining campaigns ({@code 0.0} when fewer than two remain)
 * @param remainingCampaigns  how many campaigns are still registered
 * @param revision            the tracker revision after the removal
 * @param occurredOn          the instant of the removal
 */
public record HistoricalHarvestRemovedEvent(
        String entryId,
        String plotId,
        Integer campaignYear,
        Double recalculatedBbi,
        Integer remainingCampaigns,
        Long revision,
        Instant occurredOn
) {

    /**
     * Canonical constructor validating the structural invariants of the event.
     */
    public HistoricalHarvestRemovedEvent {
        if (entryId == null || entryId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.entry_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.plot_id.null");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("phenology.event.occurred_on.null");
        }
    }
}
