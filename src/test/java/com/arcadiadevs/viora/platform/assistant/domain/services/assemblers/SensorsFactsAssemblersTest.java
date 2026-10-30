package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Coverage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.climate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.data;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.microclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.realProbe;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("SENSORS facts assemblers")
class SensorsFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.sensors.";
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 10, 29);
    private final SensorsSayFactsAssembler whatSay = new SensorsSayFactsAssembler(QuestionId.SENSORS_WHAT_SAY);
    private final SensorsReliabilityFactsAssembler reliability = new SensorsReliabilityFactsAssembler();
    private final SensorsHowLinkFactsAssembler howLink = new SensorsHowLinkFactsAssembler();

    @Test
    @DisplayName("what_say, reliability and how_link are registered; why_paused has no assembler (always disabled)")
    void registered() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.SENSORS_WHAT_SAY, QuestionId.SENSORS_RELIABILITY, QuestionId.SENSORS_HOW_LINK))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
        assertThat(registry.find(QuestionId.SENSORS_WHY_PAUSED)).isEmpty();
        assertThat(registry.find(QuestionId.CLIMATE_SENSORS_SAY)).map(FactsAssembler::questionId)
                .contains(QuestionId.CLIMATE_SENSORS_SAY);
    }

    @Test
    @DisplayName("what_say with the contract example: 2 nodes, 27,4 °C, labeled simulated")
    void whatSayContractExample() {
        var facts = whatSay.assemble(sensors(List.of(microclimate(), microclimate()), reading(21.5), 1)).orElseThrow();

        assertThat(facts.facts()).contains(entry("linkedNodes", 2), entry("airTemperature", 27.4),
                entry("simulated", true));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "what_say.text.simulated", 2, 27.4, 38L, 21.5));
        assertThat(facts.cards().get(0).pill().key()).isEqualTo("assistant.template.card.simulated.pill");
    }

    @Test
    @DisplayName("reliability of simulated nodes: origin first, then the days with readings out of 7")
    void reliabilitySimulated() {
        var facts = reliability.assemble(sensors(List.of(microclimate(), microclimate()), reading(21.5), 5))
                .orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("daysWithReadings", 5), entry("lastReadingOn", "2026-10-29"),
                entry("linkedNodes", 2), entry("simulated", true), entry("windowDays", 7));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "reliability.text",
                CatalogText.of(PREFIX + "reliability.origin.simulated"),
                CatalogText.of(PREFIX + "reliability.coverage.days", 5, 7, TextDate.of(YESTERDAY))));
        assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of("assistant.template.value.fraction", 5, 7));
        assertThat(facts.cards().get(1).value().key()).isEqualTo("assistant.template.card.origin.simulated");
        assertThat(facts.sources()).extracting(CatalogText::key)
                .containsExactly("assistant.template.source.virtual_node");
    }

    @Test
    @DisplayName("reliability of a measured probe without readings in the window; nothing without nodes")
    void reliabilityMeasuredWithoutReadings() {
        var data = data(new ClimateData(THRESHOLDS, null, 0, 0, null, List.of(realProbe()), null,
                new Coverage(null, 0, 7)));
        var facts = reliability.assemble(data).orElseThrow();

        assertThat(facts.facts()).doesNotContainKey("lastReadingOn").contains(entry("simulated", false));
        assertThat(facts.text().args()).containsExactly(CatalogText.of(PREFIX + "reliability.origin.measured", 1),
                CatalogText.of(PREFIX + "reliability.coverage.none", 7));
        assertThat(facts.sources()).containsExactly(CatalogText.of("assistant.template.source.nodes", 1));

        assertThat(reliability.assemble(sensors(List.of(), null, 0))).isEmpty();
    }

    @Test
    @DisplayName("how_link counts the nodes by type and gives the steps Telemetry asks for, with or without nodes")
    void howLink() {
        var some = howLink.assemble(sensors(List.of(microclimate(), realProbe()), reading(21.5), 1)).orElseThrow();
        assertThat(some.facts()).containsExactly(entry("linkedNodes", 2), entry("microclimateNodes", 1),
                entry("soilProbes", 1));
        assertThat(some.text()).isEqualTo(CatalogText.of(PREFIX + "how_link.text",
                CatalogText.of(PREFIX + "how_link.state.some", 2), CatalogText.of(PREFIX + "how_link.steps")));
        assertThat(some.actions()).isEmpty();

        var none = howLink.assemble(sensors(List.of(), null, 0)).orElseThrow();
        assertThat(none.text().args().getFirst()).isEqualTo(CatalogText.of(PREFIX + "how_link.state.none"));
        assertThat(none.cards()).extracting(card -> card.value())
                .containsExactly(CatalogText.of("assistant.template.value.integer", 0),
                        CatalogText.of("assistant.template.value.integer", 0));
    }

    @Test
    @DisplayName("Climate data without coverage (CLIMATE, ALERT) never answers reliability or how_link")
    void onlySensorsData() {
        var climateScreen = data(climate(null, null, List.of(microclimate()), reading(21.5)));

        assertThat(reliability.assemble(climateScreen)).isEmpty();
        assertThat(howLink.assemble(climateScreen)).isEmpty();
    }

    private static PlotAnswerData sensors(List<SensorNode> nodes, ClimateData.Reading reading, int days) {
        return data(new ClimateData(THRESHOLDS, null, 0, 0, null, nodes, reading,
                new Coverage(days > 0 ? YESTERDAY : null, days, 7)));
    }
}
