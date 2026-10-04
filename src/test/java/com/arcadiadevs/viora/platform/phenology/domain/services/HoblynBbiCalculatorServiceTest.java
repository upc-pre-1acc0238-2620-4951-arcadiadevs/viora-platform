package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntry;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BbiAlternationCategory;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BiennialBearingIndex;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HoblynBbiCalculatorService Domain Service Tests")
class HoblynBbiCalculatorServiceTest {

    @Test
    @DisplayName("Should evaluate sample sufficiency correctly (SUFFICIENT if >= 3, INSUFFICIENT otherwise)")
    void shouldEvaluateSampleSufficiencyCorrectly() {
        assertThat(HoblynBbiCalculatorService.evaluateSufficiency(0).isSufficient()).isFalse();
        assertThat(HoblynBbiCalculatorService.evaluateSufficiency(1).isSufficient()).isFalse();
        assertThat(HoblynBbiCalculatorService.evaluateSufficiency(2).isSufficient()).isFalse();
        assertThat(HoblynBbiCalculatorService.evaluateSufficiency(3).isSufficient()).isTrue();
        assertThat(HoblynBbiCalculatorService.evaluateSufficiency(5).isSufficient()).isTrue();
    }

    @Test
    @DisplayName("Should return 0.0 and INSUFFICIENT_DATA when entries size is less than 2")
    void shouldReturnZeroAndInsufficientDataWhenFewerThanTwoEntries() {
        var emptyResult = HoblynBbiCalculatorService.calculateBbi(List.of());
        assertThat(emptyResult.value()).isEqualTo(0.0);
        assertThat(HoblynBbiCalculatorService.classifyAlternation(emptyResult, 0))
                .isEqualTo(BbiAlternationCategory.INSUFFICIENT_DATA);

        var singleEntry = List.of(HistoricalHarvestEntry.create(new CampaignYear(2022), HarvestYield.ofTotal(10000.0)));
        var singleResult = HoblynBbiCalculatorService.calculateBbi(singleEntry);
        assertThat(singleResult.value()).isEqualTo(0.0);
        assertThat(HoblynBbiCalculatorService.classifyAlternation(singleResult, 1))
                .isEqualTo(BbiAlternationCategory.INSUFFICIENT_DATA);
    }

    @Test
    @DisplayName("Should calculate exact BBI and classify as REGULAR when variation is low (< 0.20)")
    void shouldClassifyAsRegularWhenBbiBelowPoint20() {
        // Year 2021: 10,000 kg, Year 2022: 12,000 kg -> |12000 - 10000| / (22000) = 2000 / 22000 = 0.091
        var entries = List.of(
                HistoricalHarvestEntry.create(new CampaignYear(2021), HarvestYield.ofTotal(10000.0)),
                HistoricalHarvestEntry.create(new CampaignYear(2022), HarvestYield.ofTotal(12000.0))
        );

        var bbi = HoblynBbiCalculatorService.calculateBbi(entries);
        assertThat(bbi.value()).isEqualTo(0.091);

        var category = HoblynBbiCalculatorService.classifyAlternation(bbi, entries.size());
        assertThat(category).isEqualTo(BbiAlternationCategory.REGULAR);
    }

    @Test
    @DisplayName("Should calculate exact BBI and classify as MODERATE_ALTERNATION when 0.20 <= BBI <= 0.40")
    void shouldClassifyAsModerateAlternationWhenBetween20And40() {
        // Year 2021: 10,000 kg, Year 2022: 5,000 kg -> |5000 - 10000| / (15000) = 0.333
        var entries = List.of(
                HistoricalHarvestEntry.create(new CampaignYear(2021), HarvestYield.ofTotal(10000.0)),
                HistoricalHarvestEntry.create(new CampaignYear(2022), HarvestYield.ofTotal(5000.0))
        );

        var bbi = HoblynBbiCalculatorService.calculateBbi(entries);
        assertThat(bbi.value()).isEqualTo(0.333);

        var category = HoblynBbiCalculatorService.classifyAlternation(bbi, entries.size());
        assertThat(category).isEqualTo(BbiAlternationCategory.MODERATE_ALTERNATION);
    }

    @Test
    @DisplayName("Should calculate exact BBI and classify as SEVERE_ALTERNATION when BBI > 0.40")
    void shouldClassifyAsSevereAlternationWhenBbiAbove40() {
        // Year 2021: 20,000 kg, Year 2022: 2,000 kg -> |2000 - 20000| / (22000) = 18000 / 22000 = 0.818
        var entries = List.of(
                HistoricalHarvestEntry.create(new CampaignYear(2021), HarvestYield.ofTotal(20000.0)),
                HistoricalHarvestEntry.create(new CampaignYear(2022), HarvestYield.ofTotal(2000.0))
        );

        var bbi = HoblynBbiCalculatorService.calculateBbi(entries);
        assertThat(bbi.value()).isEqualTo(0.818);

        var category = HoblynBbiCalculatorService.classifyAlternation(bbi, entries.size());
        assertThat(category).isEqualTo(BbiAlternationCategory.SEVERE_ALTERNATION);
    }

    @ParameterizedTest(name = "BBI {0} classifies as {1}")
    @CsvSource({
            "0.19, REGULAR",
            "0.20, MODERATE_ALTERNATION",
            "0.40, MODERATE_ALTERNATION",
            "0.41, SEVERE_ALTERNATION"
    })
    @DisplayName("Should classify the exact 0.20 and 0.40 band boundaries inclusively as MODERATE_ALTERNATION")
    void shouldClassifyBandBoundaries(double bbiValue, BbiAlternationCategory expected) {
        // The bands are closed on the moderate side: 0.20 and 0.40 stay MODERATE, only 0.41 flips to SEVERE
        var category = HoblynBbiCalculatorService.classifyAlternation(new BiennialBearingIndex(bbiValue), 3);

        assertThat(category).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should correctly sort unsorted campaign year entries before BBI computation")
    void shouldSortUnsortedEntriesBeforeCalculating() {
        var entries = List.of(
                HistoricalHarvestEntry.create(new CampaignYear(2023), HarvestYield.ofTotal(10000.0)),
                HistoricalHarvestEntry.create(new CampaignYear(2021), HarvestYield.ofTotal(10000.0)),
                HistoricalHarvestEntry.create(new CampaignYear(2022), HarvestYield.ofTotal(5000.0))
        );

        var bbi = HoblynBbiCalculatorService.calculateBbi(entries);
        // Sorted: 2021 (10k), 2022 (5k), 2023 (10k)
        // Pair 1: |5 - 10| / 15 = 0.333
        // Pair 2: |10 - 5| / 15 = 0.333
        // Average: 0.333
        assertThat(bbi.value()).isEqualTo(0.333);
    }
}
