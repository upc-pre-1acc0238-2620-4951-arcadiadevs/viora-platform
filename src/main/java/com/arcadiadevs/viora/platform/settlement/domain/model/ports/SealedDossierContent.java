package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Frozen data of the dossier a producer seals for one settled campaign; the PDF renderer only reads it.
 *
 * @param plotId           plot of the campaign
 * @param producerId       owner of the plot
 * @param campaignYear     sealed campaign
 * @param facts            values the dossier states
 * @param settlement       settlement of the campaign (weights, receipt, thinning balance and stabilization curve)
 * @param harvestHistoryKg kilograms per harvested campaign up to this one, ascending
 * @param sealedAt         instant of the seal
 */
public record SealedDossierContent(
        PlotId plotId,
        UserId producerId,
        CampaignYear campaignYear,
        DossierFacts facts,
        HarvestSettlementSnapshot settlement,
        SortedMap<Integer, Double> harvestHistoryKg,
        Instant sealedAt
) {

    public SealedDossierContent {
        if (plotId == null || producerId == null || campaignYear == null || facts == null || settlement == null
                || sealedAt == null) {
            throw new IllegalArgumentException("settlement.dossier.content.invalid");
        }
        harvestHistoryKg = Collections.unmodifiableSortedMap(
                new TreeMap<>(harvestHistoryKg == null ? new TreeMap<>() : harvestHistoryKg));
    }
}
