package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Coverage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.microclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.reading;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: SENSORS availability")
class SensorsQuestionCatalogTest {

    @Test
    @DisplayName("SENSORS is a plot screen that reads Telemetry, with the prefix sensors.")
    void screen() {
        assertThat(ScreenKey.fromValue("sensors")).isEqualTo(ScreenKey.SENSORS);
        assertThat(ScreenKey.SENSORS.requiresPlot()).isTrue();
        assertThat(ScreenKey.SENSORS.readsClimate()).isTrue();
        assertThat(ScreenKey.SENSORS.questionPrefix()).isEqualTo("sensors.");
        assertThat(QuestionCatalog.screenOf(QuestionId.SENSORS_HOW_LINK)).contains(ScreenKey.SENSORS);
        assertThat(QuestionCatalog.screenOf(QuestionId.CLIMATE_SENSORS_SAY)).contains(ScreenKey.CLIMATE);
    }

    @Test
    @DisplayName("Nodes reading recently: contract order, all enabled but why_paused (no node can be paused yet)")
    void nodesReading() {
        var questions = sensors(List.of(microclimate(), microclimate()), reading(21.5));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "sensors.what_say", "sensors.why_paused", "sensors.reliability", "sensors.how_link");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("sensor", "alert", "history",
                "lote");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, true);
        assertThat(questions.get(1).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.no_paused_nodes"));
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.sensors.what_say.label"));
    }

    @Test
    @DisplayName("Nodes without a recent reading: only what_say waits for readings")
    void nodesWithoutReading() {
        var questions = sensors(List.of(microclimate()), null);

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, false, true, true);
        assertThat(questions.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
    }

    @Test
    @DisplayName("No nodes: only how_link is offered")
    void noNodes() {
        var questions = sensors(List.of(), null);

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, false, false, true);
        assertThat(questions.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.nodes_missing"));
        assertThat(questions.get(2).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.nodes_missing"));
    }

    private static List<CatalogQuestion> sensors(List<SensorNode> nodes, ClimateData.Reading reading) {
        return QuestionCatalog.questionsFor(ScreenKey.SENSORS, PlotFacts.ofClimate(2026,
                new ClimateData(THRESHOLDS, null, 0, 0, null, nodes, reading, new Coverage(null, 0, 7))));
    }
}
