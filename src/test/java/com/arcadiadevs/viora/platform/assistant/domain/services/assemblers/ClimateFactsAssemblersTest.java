package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("CLIMATE facts assemblers")
class ClimateFactsAssemblersTest {

    @Test
    @DisplayName("climate.irrigate: the real probe reading against its thresholds; advisable only when critical")
    void irrigate() {
        var warning = new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(realProbe("WARNING")),
                reading(18.5)))).orElseThrow();
        assertThat(warning.facts()).containsExactly(
                entry("criticalMoisture", 16.0), entry("irrigationAdvisable", false), entry("moistureState",
                        "WARNING"), entry("probeDepthCm", 30), entry("soilMoisture", 18.5),
                entry("warningMoisture", 20.0));
        assertThat(warning.text().key()).isEqualTo("assistant.template.climate.irrigate.text.warning");
        assertTwoCards(warning);
        assertThat(warning.sources()).extracting(source -> source.key())
                .containsExactly("assistant.template.source.probe");
        assertThat(warning.actions()).isEmpty();

        var critical = new IrrigateFactsAssembler().assemble(data(climate(null, null,
                List.of(realProbe("CRITICAL")), reading(14.0)))).orElseThrow();
        assertThat(critical.facts()).containsEntry("irrigationAdvisable", true).containsEntry("moistureState",
                "CRITICAL");
        assertThat(critical.text().key()).isEqualTo("assistant.template.climate.irrigate.text.critical");

        var ok = new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(realProbe(null)),
                reading(24.0)))).orElseThrow();
        assertThat(ok.facts()).containsEntry("irrigationAdvisable", false).containsEntry("moistureState", "OK");
        assertThat(ok.text().key()).isEqualTo("assistant.template.climate.irrigate.text.ok");
    }

    @Test
    @DisplayName("climate.irrigate never answers from a virtual probe or without a reading")
    void irrigateNeedsRealProbe() {
        assertThat(new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(virtualProbe()),
                reading(14.0))))).isEmpty();
        assertThat(new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(realProbe()), null))))
                .isEmpty();
    }

    @Test
    @DisplayName("climate.fruitset_effect: hottest forecast day and days over the threshold, no effect estimated")
    void fruitsetEffect() {
        var above = new ClimateFruitsetEffectFactsAssembler().assemble(data(climate(null, hotThenMild(), List.of(),
                null))).orElseThrow();
        assertThat(above.facts()).containsExactly(entry("forecastDays", 7), entry("heatDays", 3),
                entry("heatThreshold", 32.0), entry("hottestMax", 35.1), entry("hottestOn", "2026-10-31"));
        assertThat(above.text().key()).isEqualTo("assistant.template.climate.fruitset_effect.text.above");
        assertTwoCards(above);

        var below = new ClimateFruitsetEffectFactsAssembler().assemble(data(climate(null,
                forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0), List.of(), null))).orElseThrow();
        assertThat(below.text().key()).isEqualTo("assistant.template.climate.fruitset_effect.text.below");
        assertThat(new ClimateFruitsetEffectFactsAssembler().assemble(data(climate(null, null, List.of(), null))))
                .isEmpty();
    }

    @Test
    @DisplayName("climate.heat_ends: ends, lasts beyond the forecast, or no heat at all")
    void heatEnds() {
        var ends = new HeatEndsFactsAssembler().assemble(data(climate(null, hotThenMild(), List.of(), null)))
                .orElseThrow();
        assertThat(ends.text().key()).isEqualTo("assistant.template.climate.heat_ends.text.ends");
        assertThat(ends.facts()).containsExactly(entry("endsOn", "2026-11-02"), entry("forecastDays", 7),
                entry("heatDays", 3), entry("heatThreshold", 32.0), entry("lastHeatOn", "2026-11-01"));
        assertTwoCards(ends);

        var beyond = new HeatEndsFactsAssembler().assemble(data(climate(null,
                forecast(30.0, 31.0, 33.0, 30.0, 31.0, 33.5, 34.0), List.of(), null))).orElseThrow();
        assertThat(beyond.text().key()).isEqualTo("assistant.template.climate.heat_ends.text.beyond");
        var clear = new HeatEndsFactsAssembler().assemble(data(climate(null,
                forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0), List.of(), null))).orElseThrow();
        assertThat(clear.text().key()).isEqualTo("assistant.template.climate.heat_ends.text.clear");
    }

    @Test
    @DisplayName("climate.sensors_say: a virtual node reading is labeled simulated")
    void sensorsSaySimulated() {
        var facts = new SensorsSayFactsAssembler().assemble(data(climate(null, null,
                List.of(virtualProbe(), microclimate()), reading(21.54)))).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("airHumidity", 38L), entry("airTemperature", 27.4),
                entry("linkedNodes", 2), entry("simulated", true), entry("soilMoisture", 21.5));
        assertThat(facts.text().key()).isEqualTo("assistant.template.climate.sensors_say.text.simulated");
        assertThat(facts.sources()).extracting(source -> source.key())
                .containsExactly("assistant.template.source.virtual_node");
        assertTwoCards(facts);
    }

    @Test
    @DisplayName("climate.sensors_say: measured readings are not labeled simulated; no nodes or reading, no answer")
    void sensorsSayMeasured() {
        var facts = new SensorsSayFactsAssembler().assemble(data(climate(null, null, List.of(realProbe()),
                reading(21.0)))).orElseThrow();
        assertThat(facts.facts()).containsEntry("simulated", false);
        assertThat(facts.text().key()).isEqualTo("assistant.template.climate.sensors_say.text.measured");

        assertThat(new SensorsSayFactsAssembler().assemble(data(climate(null, null, List.of(), reading(21.0)))))
                .isEmpty();
        assertThat(new SensorsSayFactsAssembler().assemble(data(climate(null, null, List.of(realProbe()), null))))
                .isEmpty();
    }

    private static void assertTwoCards(AssembledFacts facts) {
        assertThat(facts.cards()).hasSize(2);
        assertThat(facts.cards()).extracting(FactCardTemplate::tone)
                .containsExactly(FactTone.PRIMARY, FactTone.HIGHLIGHT);
    }
}
