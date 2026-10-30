package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("ALERT facts assemblers")
class AlertFactsAssemblersTest {

    @Test
    @DisplayName("alert.what_to_do: the breach against the threshold and the mitigation steps done")
    void whatToDo() {
        var facts = new AlertWhatToDoFactsAssembler().assemble(data(climate(heat(), null, List.of(), null)))
                .orElseThrow();

        assertThat(new AlertWhatToDoFactsAssembler().questionId()).isEqualTo(QuestionId.ALERT_WHAT_TO_DO);
        assertThat(facts.facts()).containsExactly(
                entry("alertRaisedOn", "2026-10-28"), entry("alertThreshold", 32.0), entry("alertValue", 34.2),
                entry("completedSteps", 1), entry("incidentType", "HEAT_WAVE"), entry("mitigationSteps", 3),
                entry("severity", "WARNING"), entry("unit", "°C"));
        assertThat(facts.text().key()).isEqualTo("assistant.template.alert.what_to_do.text");
        assertTwoCards(facts);
        assertThat(facts.sources()).extracting(source -> source.key())
                .containsExactly("assistant.template.source.alert");
        assertThat(facts.actions()).isEmpty();
    }

    @Test
    @DisplayName("alert.what_to_do without mitigation steps uses its own text")
    void whatToDoWithoutSteps() {
        var facts = new AlertWhatToDoFactsAssembler().assemble(data(climate(hydric(), null, List.of(), null)))
                .orElseThrow();

        assertThat(facts.text().key()).isEqualTo("assistant.template.alert.what_to_do.text.no_steps");
        assertThat(facts.facts()).containsEntry("unit", "%").containsEntry("alertValue", 18.5);
    }

    @Test
    @DisplayName("Every ALERT assembler needs an open alert")
    void needsAnAlert() {
        var none = data(climate(null, hotThenMild(), List.of(), null));

        assertThat(new AlertWhatToDoFactsAssembler().assemble(none)).isEmpty();
        assertThat(new AlertFruitsetEffectFactsAssembler().assemble(none)).isEmpty();
        assertThat(new AlertWhenEndsFactsAssembler().assemble(none)).isEmpty();
        assertThat(new AlertPlotsAtRiskFactsAssembler().assemble(none)).isEmpty();
    }

    @Test
    @DisplayName("alert.fruitset_effect: only the breach; the platform does not estimate the effect on fruit set")
    void fruitsetEffect() {
        var facts = new AlertFruitsetEffectFactsAssembler().assemble(data(climate(heat(), null, List.of(), null)))
                .orElseThrow();

        assertThat(facts.text().key()).isEqualTo("assistant.template.alert.fruitset_effect.text");
        assertThat(facts.facts()).containsEntry("alertValue", 34.2).containsEntry("alertThreshold", 32.0)
                .doesNotContainKey("fruitSetEffect");
        assertTwoCards(facts);
    }

    @Test
    @DisplayName("alert.when_ends: the heat lasts three days and ends on the fourth, from Telemetry's flags")
    void whenEndsHeat() {
        var facts = new AlertWhenEndsFactsAssembler().assemble(data(climate(heat(), hotThenMild(), List.of(), null)))
                .orElseThrow();

        assertThat(facts.text().key()).isEqualTo("assistant.template.alert.when_ends.text.ends");
        assertThat(facts.facts()).contains(entry("riskThreshold", 32.0), entry("forecastDays", 7),
                entry("riskDays", 3), entry("lastRiskOn", "2026-11-01"), entry("endsOn", "2026-11-02"));
        assertTwoCards(facts);
        assertThat(facts.sources()).extracting(source -> source.key())
                .containsExactly("assistant.template.source.forecast");
    }

    @Test
    @DisplayName("alert.when_ends: risk on the last forecast day, or no risk at all, have their own texts")
    void whenEndsBeyondOrClear() {
        var beyond = new AlertWhenEndsFactsAssembler().assemble(data(climate(heat(),
                forecast(30.0, 31.0, 33.0, 30.0, 31.0, 33.5, 34.0), List.of(), null))).orElseThrow();
        assertThat(beyond.text().key()).isEqualTo("assistant.template.alert.when_ends.text.beyond");
        assertThat(beyond.facts()).contains(entry("riskDays", 3), entry("lastRiskOn", "2026-11-05"))
                .doesNotContainKey("endsOn");

        var clear = new AlertWhenEndsFactsAssembler().assemble(data(climate(heat(),
                forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0), List.of(), null))).orElseThrow();
        assertThat(clear.text().key()).isEqualTo("assistant.template.alert.when_ends.text.clear");
        assertThat(clear.facts()).containsEntry("riskDays", 0).doesNotContainKey("lastRiskOn");
    }

    @Test
    @DisplayName("alert.when_ends of a frost uses the frost flags and threshold; a hydric alert has no answer")
    void whenEndsFrostAndHydric() {
        var frost = new AlertWhenEndsFactsAssembler().assemble(data(climate(frost(),
                forecast(9.0, 8.5, 12.0, 15.0, 16.0, 18.0, 19.0), List.of(), null))).orElseThrow();
        assertThat(frost.facts()).contains(entry("riskThreshold", 1.5), entry("riskDays", 2),
                entry("endsOn", "2026-11-01"));

        assertThat(new AlertWhenEndsFactsAssembler().assemble(data(climate(hydric(), hotThenMild(), List.of(),
                null)))).isEmpty();
        assertThat(new AlertWhenEndsFactsAssembler().assemble(data(climate(heat(), null, List.of(), null))))
                .isEmpty();
    }

    @Test
    @DisplayName("alert.plots_at_risk: plots of the producer with the same alert, this one included")
    void plotsAtRisk() {
        var many = new AlertPlotsAtRiskFactsAssembler().assemble(data(climate(heat(), null, List.of(), null)))
                .orElseThrow();
        assertThat(many.text().key()).isEqualTo("assistant.template.alert.plots_at_risk.text.many");
        assertThat(many.facts()).contains(entry("plotsAtRisk", 2), entry("activePlots", 5));
        assertTwoCards(many);

        var only = new AlertPlotsAtRiskFactsAssembler().assemble(data(new ClimateData(THRESHOLDS, heat(), 1, 5,
                null, List.of(), null))).orElseThrow();
        assertThat(only.text().key()).isEqualTo("assistant.template.alert.plots_at_risk.text.only");
        var single = new AlertPlotsAtRiskFactsAssembler().assemble(data(new ClimateData(THRESHOLDS, heat(), 1, 1,
                null, List.of(), null))).orElseThrow();
        assertThat(single.text().key()).isEqualTo("assistant.template.alert.plots_at_risk.text.single");
        assertThat(new AlertPlotsAtRiskFactsAssembler().assemble(data(new ClimateData(THRESHOLDS, heat(), 0, 5,
                null, List.of(), null)))).isEmpty();
    }

    private static void assertTwoCards(AssembledFacts facts) {
        assertThat(facts.cards()).hasSize(2);
        assertThat(facts.cards()).extracting(FactCardTemplate::tone)
                .containsExactly(FactTone.PRIMARY, FactTone.HIGHLIGHT);
    }
}
