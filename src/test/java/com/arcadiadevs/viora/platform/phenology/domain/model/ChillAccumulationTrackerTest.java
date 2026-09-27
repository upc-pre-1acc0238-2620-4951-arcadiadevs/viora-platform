package com.arcadiadevs.viora.platform.phenology.domain.model;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.BiennialBearingIndexAssessedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.HarvestYieldRecordedEvent;
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
}
