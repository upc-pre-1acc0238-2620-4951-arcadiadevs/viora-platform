package com.arcadiadevs.viora.platform.phenology.domain.model.aggregates;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BearingClassification;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestEntryId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;

import java.time.Instant;

/**
 * Pure domain internal entity representing an annual harvest record in the olive orchard.
 * Subordinated exclusively to the {@link ChillAccumulationTracker} aggregate root.
 */
public class HistoricalHarvestEntry {

    private final HarvestEntryId id;
    private final CampaignYear campaignYear;
    private HarvestYield harvestYield;
    private BearingClassification classification;
    private final Instant recordedAt;

    private HistoricalHarvestEntry(
            HarvestEntryId id,
            CampaignYear campaignYear,
            HarvestYield harvestYield,
            BearingClassification classification,
            Instant recordedAt
    ) {
        this.id = id;
        this.campaignYear = campaignYear;
        this.harvestYield = harvestYield;
        this.classification = classification;
        this.recordedAt = recordedAt;
    }

    /**
     * Domain factory method initializing a new harvest entry.
     *
     * @param campaignYear the campaign year
     * @param harvestYield the harvest yield
     * @return a new consistent {@link HistoricalHarvestEntry}
     */
    public static HistoricalHarvestEntry create(CampaignYear campaignYear, HarvestYield harvestYield) {
        return new HistoricalHarvestEntry(
                new HarvestEntryId(),
                campaignYear,
                harvestYield,
                BearingClassification.INSUFFICIENT_DATA,
                Instant.now()
        );
    }

    /**
     * Reconstitutes an existing entity instance from an immutable snapshot.
     *
     * @param snapshot the entity snapshot
     * @return the reconstituted entity
     */
    public static HistoricalHarvestEntry reconstitute(HistoricalHarvestEntrySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("phenology.harvest_entry.not_found");
        }
        return new HistoricalHarvestEntry(
                snapshot.id(),
                snapshot.campaignYear(),
                snapshot.harvestYield(),
                snapshot.classification(),
                snapshot.recordedAt()
        );
    }

    /**
     * Updates the harvest yield of this campaign entry.
     *
     * @param updatedHarvestYield the updated harvest yield
     */
    public void updateYield(HarvestYield updatedHarvestYield) {
        this.harvestYield = updatedHarvestYield;
    }

    /**
     * Assigns the bearing classification relative to the orchard's historical mean.
     *
     * @param averageYield the historical mean fruit yield
     */
    public void classify(Double averageYield) {
        if (averageYield == null || averageYield <= 0.0 || this.harvestYield == null || this.harvestYield.totalKg() == null) {
            this.classification = BearingClassification.INSUFFICIENT_DATA;
            return;
        }
        double ratio = this.harvestYield.totalKg() / averageYield;
        if (ratio >= 1.25) {
            this.classification = BearingClassification.ON_YEAR;
        } else if (ratio <= 0.75) {
            this.classification = BearingClassification.OFF_YEAR;
        } else {
            this.classification = BearingClassification.BALANCED;
        }
    }

    /**
     * Captures an immutable snapshot of this entity.
     *
     * @return a {@link HistoricalHarvestEntrySnapshot}
     */
    public HistoricalHarvestEntrySnapshot snapshot() {
        return new HistoricalHarvestEntrySnapshot(id, campaignYear, harvestYield, classification, recordedAt);
    }
}
