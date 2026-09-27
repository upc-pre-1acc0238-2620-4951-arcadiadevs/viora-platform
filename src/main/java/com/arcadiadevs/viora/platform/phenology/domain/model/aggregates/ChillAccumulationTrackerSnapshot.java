package com.arcadiadevs.viora.platform.phenology.domain.model.aggregates;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BiennialBearingIndex;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.TrackerId;

import java.util.List;

/**
 * Immutable snapshot capturing the complete state of a {@code ChillAccumulationTracker} aggregate root.
 *
 * @param id              the aggregate tracker identifier
 * @param plotId          the logical plot identifier
 * @param currentCampaign the active monitoring campaign year
 * @param harvestHistory  the list of historical harvest entry snapshots
 * @param calculatedBbi   the assessed Hoblyn Biennial Bearing Index
 * @param revision        the optimistic concurrency revision
 */
public record ChillAccumulationTrackerSnapshot(
        TrackerId id,
        PlotId plotId,
        CampaignYear currentCampaign,
        List<HistoricalHarvestEntrySnapshot> harvestHistory,
        BiennialBearingIndex calculatedBbi,
        Long revision
) {
}
