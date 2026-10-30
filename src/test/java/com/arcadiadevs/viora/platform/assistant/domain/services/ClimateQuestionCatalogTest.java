package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: ALERT and CLIMATE availability")
class ClimateQuestionCatalogTest {

    @Test
    @DisplayName("ALERT and CLIMATE are plot screens whose questions use their prefix")
    void screens() {
        assertThat(ScreenKey.fromValue("alert")).isEqualTo(ScreenKey.ALERT);
        assertThat(ScreenKey.fromValue("CLIMATE")).isEqualTo(ScreenKey.CLIMATE);
        assertThat(ScreenKey.ALERT.requiresPlot()).isTrue();
        assertThat(QuestionCatalog.screenOf(QuestionId.ALERT_WHEN_ENDS)).contains(ScreenKey.ALERT);
        assertThat(QuestionCatalog.screenOf(QuestionId.CLIMATE_SENSORS_SAY)).contains(ScreenKey.CLIMATE);
    }

    @Test
    @DisplayName("ALERT with a heat alert and a forecast: four questions in contract order, all enabled")
    void alertWithHeat() {
        var questions = alert(climate(heat(), hotThenMild(), List.of(), null));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "alert.what_to_do", "alert.fruitset_effect", "alert.when_ends", "alert.plots_at_risk");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("alert", "fruit", "calendar", "lote");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.alert.what_to_do.label.HEAT_WAVE"));
    }

    @Test
    @DisplayName("ALERT without an open alert disables every question: \"Sin alertas activas en este lote\"")
    void alertWithoutIncident() {
        var questions = alert(climate(null, hotThenMild(), List.of(), null));

        assertThat(questions).noneMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::disabledReason)
                .containsOnly(CatalogText.of("assistant.reason.alert_missing"));
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.alert.what_to_do.label"));
    }

    @Test
    @DisplayName("alert.when_ends needs a forecast, and the forecast says nothing about soil moisture")
    void alertWhenEnds() {
        assertThat(alert(climate(heat(), null, List.of(), null)).get(2).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.forecast_missing"));
        var hydric = alert(climate(hydric(), hotThenMild(), List.of(), null));
        assertThat(hydric.get(2).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.forecast_soil"));
        assertThat(hydric).extracting(CatalogQuestion::enabled).containsExactly(true, true, false, true);
        assertThat(alert(climate(frost(), hotThenMild(), List.of(), null)).get(2).enabled()).isTrue();
    }

    @Test
    @DisplayName("CLIMATE with virtual nodes only: irrigation disabled, \"Requiere una sonda de humedad real\"")
    void climateWithVirtualNodes() {
        var questions = climateScreen(climate(null, hotThenMild(), List.of(virtualProbe(), microclimate()),
                reading(18.0)));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "climate.irrigate", "climate.fruitset_effect", "climate.heat_ends", "climate.sensors_say");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("water", "fruit", "temp", "sensor");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, true, true, true);
        assertThat(questions.get(0).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.real_probe_missing"));
    }

    @Test
    @DisplayName("CLIMATE with a real probe reading recently enables irrigation")
    void climateWithRealProbe() {
        var questions = climateScreen(climate(null, hotThenMild(), List.of(realProbe()), reading(18.0)));

        assertThat(questions.get(0).enabled()).isTrue();
        assertThat(questions.get(0).disabledReason()).isNull();
    }

    @Test
    @DisplayName("CLIMATE without forecast, nodes or recent readings disables the questions that need them")
    void climateWithoutData() {
        var none = climateScreen(climate(null, null, List.of(), null));
        assertThat(none).noneMatch(CatalogQuestion::enabled);
        assertThat(none).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.real_probe_missing"),
                CatalogText.of("assistant.reason.forecast_missing"),
                CatalogText.of("assistant.reason.forecast_missing"),
                CatalogText.of("assistant.reason.nodes_missing"));

        var stale = climateScreen(climate(null, null, List.of(realProbe()), null));
        assertThat(stale.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
        assertThat(stale.get(3).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
    }

    private static List<CatalogQuestion> alert(ClimateData climate) {
        return QuestionCatalog.questionsFor(ScreenKey.ALERT, PlotFacts.ofClimate(2026, climate));
    }

    private static List<CatalogQuestion> climateScreen(ClimateData climate) {
        return QuestionCatalog.questionsFor(ScreenKey.CLIMATE, PlotFacts.ofClimate(2026, climate));
    }
}
