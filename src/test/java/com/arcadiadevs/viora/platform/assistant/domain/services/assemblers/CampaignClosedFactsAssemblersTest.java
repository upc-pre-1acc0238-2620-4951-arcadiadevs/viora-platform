package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData.PlotHome;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.harvests;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withYields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("CAMPAIGN_CLOSED facts assemblers")
class CampaignClosedFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.campaign.";
    private static final String SHARED = "assistant.template.";
    private static final LocalDate EXECUTED = LocalDate.of(2025, 11, 3);
    private final CampaignVsPreviousFactsAssembler vsPrevious = new CampaignVsPreviousFactsAssembler();
    private final CampaignPlanNextFactsAssembler planNext = new CampaignPlanNextFactsAssembler();

    @Test
    @DisplayName("vs_previous and plan_next are registered, caliber_change never is; nothing without a harvest")
    void registeredAndNeedTheHarvest() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.CAMPAIGN_VS_PREVIOUS, QuestionId.CAMPAIGN_PLAN_NEXT))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
        assertThat(registry.find(QuestionId.CAMPAIGN_CALIBER_CHANGE)).isEmpty();
        assertThat(List.<FactsAssembler>of(vsPrevious, planNext))
                .allSatisfy(assembler -> assertThat(assembler.assemble(closed(bare(), null, 1.5))).isEmpty());
        assertThat(vsPrevious.assemble(closed(withYields(harvests(2025, 12450.0), null, null), null, 1.5)))
                .isEmpty();
    }

    @Test
    @DisplayName("vs_previous with the contract figures: 8.3 t/ha, 36 % above the average of the earlier campaigns")
    void vsPreviousInTonsPerHectare() {
        var facts = vsPrevious.assemble(closed(withYields(harvests(2022, 8700.0, 9150.0, 9600.0, 12450.0), null,
                null), null, 1.5)).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("areaHectares", 1.5), entry("differencePercent", 36L),
                entry("harvestCampaigns", 4), entry("lastCampaignKg", 12450L), entry("lastCampaignTonsPerHa", 8.3),
                entry("lastCampaignYear", 2025), entry("previousAverageKg", 9150L),
                entry("previousAverageTonsPerHa", 6.1), entry("previousCampaigns", 3), entry("trend", "ABOVE"));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "vs_previous.text.above", "2025", 8.3, 12450L,
                36L, 3, 6.1));
        assertThat(facts.cards()).hasSize(2);
        assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of(SHARED + "value.t_per_ha", 8.3));
        assertThat(facts.cards().get(0).label()).isEqualTo(CatalogText.of(SHARED + "card.harvest.label", "2025"));
        assertThat(facts.cards().get(0).pill()).isEqualTo(CatalogText.of(SHARED + "card.average_t_per_ha.pill", 6.1));
        assertThat(facts.cards().get(0).tone()).isEqualTo(FactTone.PRIMARY);
        assertThat(facts.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "value.plus_percent", 36L));
        assertThat(facts.cards().get(1).label()).isEqualTo(CatalogText.of(SHARED + "card.vs_average.label"));
        assertThat(facts.cards().get(1).pill()).isEqualTo(CatalogText.of(SHARED + "card.previous.pill", 3));
        assertThat(facts.sources()).containsExactly(CatalogText.of(SHARED + "source.campaigns", 4),
                CatalogText.of(SHARED + "source.plot_area", 1.5));
    }

    @Test
    @DisplayName("vs_previous without the plot area stays in kilograms; below and equal have their own texts")
    void vsPreviousInKilograms() {
        var kg = vsPrevious.assemble(closed(withYields(harvests(2024, 10000.0, 7000.0), null, null), null, null))
                .orElseThrow();
        assertThat(kg.facts()).doesNotContainKeys("areaHectares", "lastCampaignTonsPerHa",
                "previousAverageTonsPerHa");
        assertThat(kg.facts()).contains(entry("trend", "BELOW"), entry("differencePercent", 30L));
        assertThat(kg.text()).isEqualTo(CatalogText.of(PREFIX + "vs_previous.text.kg.below", "2025", 7000L, 30L, 1,
                10000L));
        assertThat(kg.cards().get(0).value()).isEqualTo(CatalogText.of(SHARED + "value.kg", 7000L));
        assertThat(kg.cards().get(0).pill()).isEqualTo(CatalogText.of(SHARED + "card.average.pill", 10000L));
        assertThat(kg.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "value.minus_percent", 30L));
        assertThat(kg.sources()).containsExactly(CatalogText.of(SHARED + "source.campaigns", 2));

        var equal = vsPrevious.assemble(closed(withYields(harvests(2024, 9000.0, 9000.0), null, null), null, 2.0))
                .orElseThrow();
        assertThat(equal.text()).isEqualTo(CatalogText.of(PREFIX + "vs_previous.text.equal", "2025", 4.5, 9000L, 0L,
                1, 4.5));
        assertThat(equal.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "value.percent", 0L));
    }

    @Test
    @DisplayName("plan_next: the band, the on-time thinning of the closed campaign and the steps; no harvest figure")
    void planNextWithIndexAndThinning() {
        var facts = planNext.assemble(closed(withYields(harvests(2022, 8700.0, 9150.0, 9600.0, 12450.0), 0.5123,
                "MODERATE_ALTERNATION"), outcome("OPTIMAL", 14, 49), 1.5)).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("bearingBand", "MODERATE_ALTERNATION"),
                entry("bearingIndex", 0.51), entry("harvestCampaigns", 4), entry("lastCampaignYear", 2025),
                entry("nextCampaignYear", 2026), entry("thinned", true), entry("thinningRecordedOn", "2025-11-03"),
                entry("thinningTimeliness", "OPTIMAL"), entry("windowClosesDaysAfterBloom", 49),
                entry("windowOpensDaysAfterBloom", 14));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "plan_next.text",
                CatalogText.of(PREFIX + "plan_next.bearing.alternating", 0.51,
                        CatalogText.of(SHARED + "band.MODERATE_ALTERNATION")),
                CatalogText.of(PREFIX + "plan_next.thinning.OPTIMAL", "2025", TextDate.of(EXECUTED)),
                CatalogText.of(PREFIX + "plan_next.steps.window", "2026", 14, 49)));
        assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of(SHARED + "card.date.value", "2026"));
        assertThat(facts.cards().get(0).label()).isEqualTo(CatalogText.of(SHARED + "card.next_campaign.label"));
        assertThat(facts.cards().get(0).pill()).isEqualTo(CatalogText.of(SHARED + "card.after_bloom.pill"));
        assertThat(facts.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "value.decimal", 0.51));
        assertThat(facts.cards().get(1).label()).isEqualTo(CatalogText.of(SHARED + "card.bbi.label"));
        assertThat(facts.cards().get(1).pill()).isEqualTo(CatalogText.of(SHARED + "band.MODERATE_ALTERNATION"));
        assertThat(facts.sources()).containsExactly(CatalogText.of(SHARED + "source.campaigns", 4),
                CatalogText.of(SHARED + "source.thinning", TextDate.of(EXECUTED)));
        assertThat(facts.facts()).doesNotContainKeys("estimatedKgPerHa", "lastCampaignKg", "lastCampaignTonsPerHa");
    }

    @Test
    @DisplayName("plan_next without index nor thinning, with a regular band, a late or an undated thinning")
    void planNextVariants() {
        var bare = planNext.assemble(closed(withYields(harvests(2025, 12450.0), null, null), null, 1.5))
                .orElseThrow();
        assertThat(bare.facts()).containsExactly(entry("harvestCampaigns", 1), entry("lastCampaignYear", 2025),
                entry("nextCampaignYear", 2026), entry("thinned", false));
        assertThat(bare.text()).isEqualTo(CatalogText.of(PREFIX + "plan_next.text.short",
                CatalogText.of(PREFIX + "plan_next.thinning.none", "2025"),
                CatalogText.of(PREFIX + "plan_next.steps", "2026")));
        assertThat(bare.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "card.not_thinned.value"));
        assertThat(bare.cards().get(1).label()).isEqualTo(CatalogText.of(SHARED + "card.closed_thinning.label",
                "2025"));
        assertThat(bare.cards().get(1).pill()).isEqualTo(CatalogText.of(SHARED + "card.no_plan.pill"));
        assertThat(bare.sources()).containsExactly(CatalogText.of(SHARED + "source.campaigns", 1));

        var late = planNext.assemble(closed(withYields(harvests(2024, 9000.0, 12450.0), 0.2, "REGULAR"),
                outcome("LATE", null, null), 1.5)).orElseThrow();
        assertThat(late.text()).isEqualTo(CatalogText.of(PREFIX + "plan_next.text",
                CatalogText.of(PREFIX + "plan_next.bearing.regular", 0.2, CatalogText.of(SHARED + "band.REGULAR")),
                CatalogText.of(PREFIX + "plan_next.thinning.LATE", "2025", TextDate.of(EXECUTED)),
                CatalogText.of(PREFIX + "plan_next.steps", "2026")));
        assertThat(late.facts()).contains(entry("thinningTimeliness", "LATE"))
                .doesNotContainKeys("windowOpensDaysAfterBloom", "windowClosesDaysAfterBloom");

        var undated = planNext.assemble(closed(withYields(harvests(2025, 12450.0), null, null),
                outcome(null, null, null), 1.5)).orElseThrow();
        assertThat(undated.text().args().getFirst())
                .isEqualTo(CatalogText.of(PREFIX + "plan_next.thinning.done", "2025", TextDate.of(EXECUTED)));
        assertThat(undated.facts()).doesNotContainKey("thinningTimeliness");
        assertThat(undated.cards().get(1).value()).isEqualTo(CatalogText.of(SHARED + "card.done.value"));
        assertThat(undated.cards().get(1).pill())
                .isEqualTo(CatalogText.of(SHARED + "card.done.pill", TextDate.shortOf(EXECUTED)));
    }

    private static ThinningOutcomeData outcome(String timeliness, Integer opens, Integer closes) {
        return new ThinningOutcomeData(EXECUTED, 30.0, timeliness, 0.57, 0.4, 0.4, "BALANCED", 9100.0, 8200.0,
                "CALIBER_ADJUSTED", 3, "PROVISIONAL", opens, closes);
    }

    private static PlotAnswerData closed(PlotHome plot, ThinningOutcomeData outcome, Double areaHectares) {
        return PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026, new HomeData(false, List.of(plot), THRESHOLDS)
                .withOutcome(outcome).withArea(areaHectares));
    }
}
