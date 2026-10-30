package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IrrigateFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SensorsSayFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Model facts: irrigation is advisable only from a critical real probe, and never ordered")
class ModelFactsTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    @Test
    @DisplayName("A critical real probe reading allows describing irrigation; the producer still decides")
    void criticalProbe() {
        var facts = new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(realProbe("CRITICAL")),
                reading(14.0)))).orElseThrow();

        var model = ModelFacts.of(new AnswerDraft(new IrrigateFactsAssembler().questionId(), ES, facts.facts(), "t",
                List.of()));

        assertThat(model).containsEntry(AnswerTextValidator.IRRIGATION_ADVISABLE, true)
                .containsEntry(AnswerTextValidator.PRODUCER_DECIDES_IRRIGATION, true)
                .containsEntry(ModelFacts.QUESTION, "climate.irrigate");
    }

    @Test
    @DisplayName("A warning reading, a sensors answer or a plan answer never allow irrigation")
    void notAdvisable() {
        var warning = new IrrigateFactsAssembler().assemble(data(climate(null, null, List.of(realProbe("WARNING")),
                reading(18.0)))).orElseThrow();
        var sensors = new SensorsSayFactsAssembler().assemble(data(climate(null, null, List.of(virtualProbe()),
                reading(18.0)))).orElseThrow();
        var plan = new WhyPercentFactsAssembler().assemble(plan(issuedPlan())).orElseThrow();

        for (var facts : List.of(warning.facts(), sensors.facts(), plan.facts())) {
            var model = ModelFacts.of(new AnswerDraft(new WhyPercentFactsAssembler().questionId(), ES, facts, "t",
                    List.of()));
            assertThat(model).containsEntry(AnswerTextValidator.IRRIGATION_ADVISABLE, false)
                    .doesNotContainKey(AnswerTextValidator.PRODUCER_DECIDES_IRRIGATION);
        }
        assertThat(sensors.facts()).containsEntry("simulated", true);
    }
}
