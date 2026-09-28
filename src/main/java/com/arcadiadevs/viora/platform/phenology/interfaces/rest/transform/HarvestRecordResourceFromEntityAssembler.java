package com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.HarvestRecordResource;

import java.util.List;

/**
 * Assembler converting domain harvest entries into public {@link HarvestRecordResource} instances.
 */
public final class HarvestRecordResourceFromEntityAssembler {

    private HarvestRecordResourceFromEntityAssembler() {
    }

    /**
     * Converts a domain harvest snapshot and contextual aggregate attributes to a presentation resource.
     *
     * @param entrySnap the harvest entry snapshot
     * @param plotId    the plot UUID string
     * @param bbiValue  the assessed BBI value
     * @return the mapped {@link HarvestRecordResource}
     */
    public static HarvestRecordResource toResource(
            HistoricalHarvestEntrySnapshot entrySnap,
            String plotId,
            Double bbiValue
    ) {
        return new HarvestRecordResource(
                entrySnap.id().harvestEntryId(),
                plotId,
                entrySnap.campaignYear().value(),
                entrySnap.harvestYield().totalKg(),
                entrySnap.harvestYield().greenKg(),
                entrySnap.harvestYield().blackKg(),
                entrySnap.classification().name(),
                bbiValue != null ? bbiValue : 0.0,
                entrySnap.recordedAt()
        );
    }

    /**
     * Converts a list of domain harvest snapshots and contextual aggregate attributes to a presentation resource list.
     *
     * @param entrySnaps the list of harvest entry snapshots
     * @param plotId     the plot UUID string
     * @param bbiValue   the assessed BBI value
     * @return the mapped list of {@link HarvestRecordResource} instances
     */
    public static List<HarvestRecordResource> toResourceList(
            List<HistoricalHarvestEntrySnapshot> entrySnaps,
            String plotId,
            Double bbiValue
    ) {
        if (entrySnaps == null) {
            return List.of();
        }
        return entrySnaps.stream()
                .map(snap -> toResource(snap, plotId, bbiValue))
                .toList();
    }
}

