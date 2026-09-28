package com.arcadiadevs.viora.platform.phenology.domain.model.events;

import java.time.Instant;

/**
 * Domain event dispatched when an existing annual olive harvest record is officially rectified,
 * updating the historical yield volume and triggering the recalculation of the pluriannual
 * Biennial Bearing Index (BBI).
 *
 * @param entryId         the identifier of the rectified harvest entry
 * @param plotId          the logical plot identifier
 * @param campaignYear    the agricultural campaign year
 * @param newTotalKg      the newly adjusted total harvested mass in kilograms
 * @param recalculatedBbi the updated Hoblyn Biennial Bearing Index
 * @param revision        the aggregate revision after rectification
 * @param occurredOn      the timestamp when the rectification occurred
 */
public record HistoricalHarvestRectifiedEvent(
        String entryId,
        String plotId,
        Integer campaignYear,
        Double newTotalKg,
        Double recalculatedBbi,
        Long revision,
        Instant occurredOn
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory event properties.
     */
    public HistoricalHarvestRectifiedEvent {
        if (entryId == null || entryId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.entry_id.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.plot_id.null");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (newTotalKg == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("phenology.event.occurred_on.null");
        }
    }
}
