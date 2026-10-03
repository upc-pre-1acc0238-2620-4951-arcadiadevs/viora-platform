package com.arcadiadevs.viora.platform.phenology.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException;
import com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.BiennialBearingIndexAssessedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HarvestYieldRecordedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HistoricalHarvestRectifiedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HistoricalHarvestRemovedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.phenology.domain.services.HoblynBbiCalculatorService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root governing biological phenology, historical bearing memory, and chilling tracking for an olive plot.
 *
 * <p>Invariant rules:
 * <ul>
 *   <li>Domain-assigned identity ({@link TrackerId}).</li>
 *   <li>Only one harvest entry per agricultural campaign year allowed in {@code harvestHistory}.</li>
 *   <li>Hoblyn's Biennial Bearing Index ($BBI$) is recalculated upon every harvest entry addition/mutation.</li>
 *   <li>Zero setters or persistence framework dependencies in domain.</li>
 *   <li>State access exclusively via immutable {@link #snapshot()}.</li>
 * </ul>
 * </p>
 */
public class ChillAccumulationTracker extends AbstractDomainAggregateRoot<ChillAccumulationTracker> {

    private final TrackerId id;
    private final PlotId plotId;
    private CampaignYear currentCampaign;
    private final List<HistoricalHarvestEntry> harvestHistory;
    private BiennialBearingIndex calculatedBbi;
    private Long revision;

    private ChillAccumulationTracker(
            TrackerId id,
            PlotId plotId,
            CampaignYear currentCampaign,
            List<HistoricalHarvestEntry> harvestHistory,
            BiennialBearingIndex calculatedBbi,
            Long revision
    ) {
        this.id = id;
        this.plotId = plotId;
        this.currentCampaign = currentCampaign;
        this.harvestHistory = new ArrayList<>(harvestHistory);
        this.calculatedBbi = calculatedBbi;
        this.revision = revision;
    }

    /**
     * Domain factory method that initializes a new phenology tracker for an orchard plot.
     *
     * @param plotId          the logical plot identifier
     * @param initialCampaign the initial campaign year of tracking
     * @return a consistent {@link ChillAccumulationTracker} instance
     */
    public static ChillAccumulationTracker create(PlotId plotId, CampaignYear initialCampaign) {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (initialCampaign == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        return new ChillAccumulationTracker(
                new TrackerId(),
                plotId,
                initialCampaign,
                new ArrayList<>(),
                BiennialBearingIndex.zero(),
                0L
        );
    }

    /**
     * Factory method used by persistence layer to reconstitute an existing aggregate.
     *
     * @param snapshot the immutable snapshot
     * @return the reconstituted aggregate root
     */
    public static ChillAccumulationTracker reconstitute(ChillAccumulationTrackerSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("phenology.tracker.snapshot.null");
        }
        var entries = snapshot.harvestHistory().stream()
                .map(HistoricalHarvestEntry::reconstitute)
                .toList();

        return new ChillAccumulationTracker(
                snapshot.id(),
                snapshot.plotId(),
                snapshot.currentCampaign(),
                entries,
                snapshot.calculatedBbi(),
                snapshot.revision()
        );
    }

    /**
     * Records an annual harvest yield for a specific campaign year, checking uniqueness
     * and recalculating the Hoblyn BBI across the complete pluriannual series.
     *
     * @param campaignYear the campaign year
     * @param harvestYield the harvest yield component
     * @return the newly created {@link HistoricalHarvestEntry}
     * @throws IllegalArgumentException if an entry for the campaign year already exists
     */
    public HistoricalHarvestEntry recordHarvest(CampaignYear campaignYear, HarvestYield harvestYield) {
        if (campaignYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (harvestYield == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }

        boolean alreadyExists = harvestHistory.stream()
                .anyMatch(e -> e.snapshot().campaignYear().value().equals(campaignYear.value()));

        if (alreadyExists) {
            throw new IllegalArgumentException("phenology.harvest_yield.duplicate_campaign");
        }

        var entry = HistoricalHarvestEntry.create(campaignYear, harvestYield);
        harvestHistory.add(entry);

        recalculateBearingMetrics();

        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        registerDomainEvent(new HarvestYieldRecordedEvent(
                entry.snapshot().id().harvestEntryId(),
                plotId.plotId(),
                campaignYear.value(),
                harvestYield.totalKg(),
                Instant.now()
        ));

        registerDomainEvent(new BiennialBearingIndexAssessedEvent(
                plotId.plotId(),
                calculatedBbi.value(),
                entry.snapshot().classification().name(),
                Instant.now()
        ));

        return entry;
    }

    /**
     * Rectifies an existing annual harvest record, validating optimistic revision concurrency,
     * updating child entity yield, and recalculating Hoblyn's BBI across the series.
     *
     * @param entryId          the identifier of the entry to rectify
     * @param rectifiedYield   the corrected harvest yield VO
     * @param expectedRevision optional optimistic revision
     * @return the updated {@link HistoricalHarvestEntry}
     * @throws com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException if revision mismatches
     * @throws com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException   if entryId is not found
     */
    public HistoricalHarvestEntry rectifyHarvestRecord(
            HarvestEntryId entryId,
            HarvestYield rectifiedYield,
            Long expectedRevision
    ) {
        if (entryId == null) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.null_or_empty");
        }
        if (rectifiedYield == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }
        if (expectedRevision != null && (this.revision == null || !expectedRevision.equals(this.revision))) {
            long current = this.revision == null ? 0L : this.revision;
            throw new TrackerRevisionMismatchException(this.id, current, expectedRevision);
        }

        var targetEntry = harvestHistory.stream()
                .filter(e -> e.snapshot().id().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new HarvestRecordNotFoundException(entryId));

        targetEntry.updateYield(rectifiedYield);

        recalculateBearingMetrics();

        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        registerDomainEvent(new HistoricalHarvestRectifiedEvent(
                targetEntry.snapshot().id().harvestEntryId(),
                plotId.plotId(),
                targetEntry.snapshot().campaignYear().value(),
                rectifiedYield.totalKg(),
                calculatedBbi.value(),
                this.revision,
                Instant.now()
        ));

        registerDomainEvent(new BiennialBearingIndexAssessedEvent(
                plotId.plotId(),
                calculatedBbi.value(),
                targetEntry.snapshot().classification().name(),
                Instant.now()
        ));

        return targetEntry;
    }

    /**
     * Removes an erroneous or duplicated harvest record from the plot's history (US21, scenario 2),
     * validating optimistic revision concurrency and recalculating Hoblyn's BBI over the campaigns
     * that remain valid.
     *
     * @param entryId          the identifier of the entry to remove
     * @param expectedRevision optional optimistic revision
     * @return the removed {@link HistoricalHarvestEntry}
     * @throws com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException if revision mismatches
     * @throws com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException   if entryId is not found
     */
    public HistoricalHarvestEntry removeHarvestRecord(HarvestEntryId entryId, Long expectedRevision) {
        if (entryId == null) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.null_or_empty");
        }
        if (expectedRevision != null && (this.revision == null || !expectedRevision.equals(this.revision))) {
            long current = this.revision == null ? 0L : this.revision;
            throw new TrackerRevisionMismatchException(this.id, current, expectedRevision);
        }

        var targetEntry = harvestHistory.stream()
                .filter(e -> e.snapshot().id().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new HarvestRecordNotFoundException(entryId));

        harvestHistory.remove(targetEntry);

        recalculateBearingMetrics();

        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        registerDomainEvent(new HistoricalHarvestRemovedEvent(
                targetEntry.snapshot().id().harvestEntryId(),
                plotId.plotId(),
                targetEntry.snapshot().campaignYear().value(),
                calculatedBbi.value(),
                harvestHistory.size(),
                this.revision,
                Instant.now()
        ));

        return targetEntry;
    }

    private void recalculateBearingMetrics() {
        if (harvestHistory.isEmpty()) {
            this.calculatedBbi = BiennialBearingIndex.zero();
            return;
        }

        double totalKgSum = harvestHistory.stream()
                .mapToDouble(e -> e.snapshot().harvestYield().totalKg())
                .sum();
        double historicalMean = totalKgSum / harvestHistory.size();

        for (var e : harvestHistory) {
            e.classify(historicalMean);
        }

        this.calculatedBbi = HoblynBbiCalculatorService.calculateBbi(harvestHistory);
    }

    /**
     * Captures an immutable snapshot of this aggregate root.
     *
     * @return a {@link ChillAccumulationTrackerSnapshot}
     */
    public ChillAccumulationTrackerSnapshot snapshot() {
        var entrySnapshots = harvestHistory.stream()
                .map(HistoricalHarvestEntry::snapshot)
                .toList();

        return new ChillAccumulationTrackerSnapshot(
                id,
                plotId,
                currentCampaign,
                entrySnapshots,
                calculatedBbi,
                revision
        );
    }
}
