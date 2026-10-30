package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Debo regar esta semana?": only from a real (not virtual) soil probe (contract, section 0). The answer states the
 * reading against the probe thresholds and leaves the decision to the producer: it never orders irrigation.
 * Irrigation advice is allowed ({@value AnswerTextValidator#IRRIGATION_ADVISABLE}) only when Telemetry judges the
 * reading critical.
 */
public final class IrrigateFactsAssembler implements FactsAssembler {

    private static final String CRITICAL = "CRITICAL";
    private static final String WARNING = "WARNING";
    private static final String OK = "OK";

    @Override
    public QuestionId questionId() {
        return QuestionId.CLIMATE_IRRIGATE;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        var reading = climate.latestReading();
        var probe = climate.realProbes().stream()
                .filter(node -> node.depthCm() != null && node.criticalMoisture() != null
                        && node.warningMoisture() != null)
                .findFirst();
        if (reading == null || probe.isEmpty()) {
            return Optional.empty();
        }
        var state = CRITICAL.equals(probe.get().moistureRisk()) ? CRITICAL
                : WARNING.equals(probe.get().moistureRisk()) ? WARNING : OK;
        int depth = probe.get().depthCm();
        double moisture = oneDecimal(reading.soilMoisture());
        double warning = oneDecimal(probe.get().warningMoisture());
        double critical = oneDecimal(probe.get().criticalMoisture());

        var facts = new TreeMap<String, Object>();
        facts.put("probeDepthCm", depth);
        facts.put("soilMoisture", moisture);
        facts.put("warningMoisture", warning);
        facts.put("criticalMoisture", critical);
        facts.put("moistureState", state);
        facts.put(AnswerTextValidator.IRRIGATION_ADVISABLE, CRITICAL.equals(state));
        var text = text(questionId(), "text." + state.toLowerCase(java.util.Locale.ROOT), depth, moisture, warning,
                critical);
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.measure", moisture, "%"), shared("card.soil_moisture.label"),
                        shared("card.probe.pill", depth), FactTone.PRIMARY),
                card(shared("value.measure", warning, "%"), shared("card.warning_moisture.label"),
                        shared("card.critical_moisture.pill", critical), FactTone.HIGHLIGHT)),
                List.of(shared("source.probe", depth)), List.of()));
    }
}
