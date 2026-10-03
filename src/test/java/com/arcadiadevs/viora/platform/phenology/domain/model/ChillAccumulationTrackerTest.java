package com.arcadiadevs.viora.platform.phenology.domain.model;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.BiennialBearingIndexAssessedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HarvestYieldRecordedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HistoricalHarvestRemovedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChillAccumulationTracker Aggregate Root Domain Unit Tests")
class ChillAccumulationTrackerTest {

    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    @Test
    @DisplayName("Should successfully record harvest entries and calculate Hoblyn BBI")
    void shouldRecordHarvestEntriesAndCalculateBbi() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));

        // 1st entry: 2022 -> 10,000 kg
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));
        assertThat(tracker.snapshot().harvestHistory()).hasSize(1);
        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.0);

        // 2nd entry: 2023 -> 5,000 kg (Alternation |5000 - 10000| / (5000 + 10000) = 5000 / 15000 = 0.333)
        tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(5000.0));
        assertThat(tracker.snapshot().harvestHistory()).hasSize(2);
        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.333);

        // Check events dispatched
        assertThat(tracker.domainEvents()).anyMatch(e -> e instanceof HarvestYieldRecordedEvent);
        assertThat(tracker.domainEvents()).anyMatch(e -> e instanceof BiennialBearingIndexAssessedEvent);
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate campaign year")
    void shouldThrowWhenDuplicateCampaignYear() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2023));
        tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(8000.0));

        assertThatThrownBy(() -> tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(7500.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.harvest_yield.duplicate_campaign");
    }

    @Test
    @DisplayName("Should throw exception when harvest yield is negative or zero")
    void shouldThrowWhenYieldIsNegativeOrZero() {
        assertThatThrownBy(() -> HarvestYield.ofTotal(0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.harvest_yield.total_positive");

        assertThatThrownBy(() -> new HarvestYield(1000.0, 800.0, 300.0)) // 800 + 300 > 1000
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.harvest_yield.incoherent_sum");
    }

    @Test
    @DisplayName("Should successfully rectify harvest record, recalculate BBI, and increment revision")
    void shouldSuccessfullyRectifyHarvestRecordAndRecalculateBbi() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        var entry1 = tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));
        var entry2 = tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(5000.0));

        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.333);
        assertThat(tracker.snapshot().revision()).isEqualTo(2L);

        // Rectify 2023 entry from 5,000 to 10,000 kg -> alternation between 10,000 and 10,000 = 0.0 BBI
        var rectified = tracker.rectifyHarvestRecord(
                entry2.snapshot().id(),
                new HarvestYield(10000.0, 6000.0, 4000.0),
                2L
        );

        assertThat(rectified.snapshot().harvestYield().totalKg()).isEqualTo(10000.0);
        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.0);
        assertThat(tracker.snapshot().revision()).isEqualTo(3L);
        assertThat(tracker.domainEvents()).anyMatch(e -> e instanceof com.arcadiadevs.viora.platform.phenology.domain.model.events.HistoricalHarvestRectifiedEvent);
    }

    @Test
    @DisplayName("Should throw TrackerRevisionMismatchException when expected revision does not match")
    void shouldThrowWhenRevisionMismatch() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        var entry = tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        assertThatThrownBy(() -> tracker.rectifyHarvestRecord(
                entry.snapshot().id(),
                HarvestYield.ofTotal(12000.0),
                99L // Outdated revision
        )).isInstanceOf(com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException.class)
          .hasMessage("phenology.tracker.revision.mismatch");
    }

    @Test
    @DisplayName("Should throw HarvestRecordNotFoundException when record id does not exist")
    void shouldThrowWhenHarvestRecordNotFound() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        var unknownId = new com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestEntryId();

        assertThatThrownBy(() -> tracker.rectifyHarvestRecord(
                unknownId,
                HarvestYield.ofTotal(12000.0),
                1L
        )).isInstanceOf(com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException.class)
          .hasMessage("phenology.harvest_entry.not_found");
    }

    @Test
    @DisplayName("Should remove a harvest record, recalculate BBI over the remaining campaigns, and increment revision")
    void shouldRemoveHarvestRecordAndRecalculateBbi() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));
        var wrong = tracker.recordHarvest(new CampaignYear(2023), HarvestYield.ofTotal(5000.0));
        tracker.recordHarvest(new CampaignYear(2024), HarvestYield.ofTotal(10000.0));
        tracker.clearDomainEvents();

        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.333);
        assertThat(tracker.snapshot().revision()).isEqualTo(3L);

        var removed = tracker.removeHarvestRecord(wrong.snapshot().id(), 3L);

        assertThat(removed.snapshot().campaignYear().value()).isEqualTo(2023);
        assertThat(tracker.snapshot().harvestHistory()).hasSize(2);
        assertThat(tracker.snapshot().harvestHistory())
                .noneMatch(e -> e.id().equals(wrong.snapshot().id()));
        // Only 2022 and 2024 remain, both 10,000 kg: no alternation.
        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.0);
        assertThat(tracker.snapshot().revision()).isEqualTo(4L);
        assertThat(tracker.domainEvents()).singleElement().isInstanceOfSatisfying(
                HistoricalHarvestRemovedEvent.class,
                event -> {
                    assertThat(event.campaignYear()).isEqualTo(2023);
                    assertThat(event.remainingCampaigns()).isEqualTo(2);
                    assertThat(event.recalculatedBbi()).isEqualTo(0.0);
                    assertThat(event.revision()).isEqualTo(4L);
                });
    }

    @Test
    @DisplayName("Should leave an empty history with a zero BBI when the only record is removed")
    void shouldLeaveEmptyHistoryWhenOnlyRecordIsRemoved() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        var only = tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        tracker.removeHarvestRecord(only.snapshot().id(), null);

        assertThat(tracker.snapshot().harvestHistory()).isEmpty();
        assertThat(tracker.snapshot().calculatedBbi().value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should allow registering again a campaign whose record was removed")
    void shouldAllowRecordingAgainARemovedCampaign() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        var wrong = tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(1000.0));
        tracker.removeHarvestRecord(wrong.snapshot().id(), null);

        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        assertThat(tracker.snapshot().harvestHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw TrackerRevisionMismatchException when removing with an outdated revision")
    void shouldThrowWhenRemovingWithRevisionMismatch() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        var entry = tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        assertThatThrownBy(() -> tracker.removeHarvestRecord(entry.snapshot().id(), 99L))
                .isInstanceOf(com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException.class)
                .hasMessage("phenology.tracker.revision.mismatch");
        assertThat(tracker.snapshot().harvestHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw HarvestRecordNotFoundException when removing an unknown record")
    void shouldThrowWhenRemovingUnknownRecord() {
        var tracker = ChillAccumulationTracker.create(plotId, new CampaignYear(2022));
        tracker.recordHarvest(new CampaignYear(2022), HarvestYield.ofTotal(10000.0));

        var unknownId = new com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestEntryId();

        assertThatThrownBy(() -> tracker.removeHarvestRecord(unknownId, null))
                .isInstanceOf(com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException.class)
                .hasMessage("phenology.harvest_entry.not_found");
    }
}
