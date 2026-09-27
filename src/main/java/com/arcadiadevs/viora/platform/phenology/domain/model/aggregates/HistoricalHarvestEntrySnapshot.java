package com.arcadiadevs.viora.platform.phenology.domain.model.aggregates;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Immutable snapshot capturing the complete state of an internal {@code HistoricalHarvestEntry}.
 *
 * @param id             the entry identifier
 * @param campaignYear   the campaign year
 * @param yield          the harvest yield component
 * @param classification the bearing classification
 * @param recordedAt     the timestamp when recorded
 */
public record HistoricalHarvestEntrySnapshot(
        HarvestEntryId id,
        CampaignYear campaignYear,
        HarvestYield harvestYield,
        BearingClassification classification,
        Instant recordedAt
) {
}
