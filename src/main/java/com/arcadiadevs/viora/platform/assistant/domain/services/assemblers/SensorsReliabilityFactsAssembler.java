package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué tan confiables son estas lecturas?": where the readings come from (simulated unless every linked node is
 * measured, as {@code climate.sensors_say} says) and on how many of the last days the plot had readings.
 */
public final class SensorsReliabilityFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.SENSORS_RELIABILITY;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        var coverage = climate.coverage();
        if (coverage == null || climate.nodes().isEmpty()) {
            return Optional.empty();
        }
        boolean simulated = climate.nodes().stream().anyMatch(SensorNode::simulated);
        int nodes = climate.nodes().size();
        var facts = new TreeMap<String, Object>();
        facts.put("linkedNodes", nodes);
        facts.put(SensorsSayFactsAssembler.SIMULATED, simulated);
        facts.put("daysWithReadings", coverage.daysWithReadings());
        facts.put("windowDays", coverage.windowDays());
        var origin = simulated
                ? text(questionId(), "origin.simulated")
                : text(questionId(), "origin.measured", nodes);
        var days = coverage.daysWithReadings() > 0 && coverage.lastReadingOn() != null
                ? text(questionId(), "coverage.days", coverage.daysWithReadings(), coverage.windowDays(),
                        TextDate.of(coverage.lastReadingOn()))
                : text(questionId(), "coverage.none", coverage.windowDays());
        if (coverage.lastReadingOn() != null) {
            facts.put("lastReadingOn", coverage.lastReadingOn().toString());
        }
        return Optional.of(new AssembledFacts(facts, text(questionId(), "text", origin, days), List.of(
                card(shared("value.fraction", coverage.daysWithReadings(), coverage.windowDays()),
                        shared("card.days_with_readings.label"),
                        shared("card.last_days.pill", coverage.windowDays()), FactTone.PRIMARY),
                card(simulated ? shared("card.origin.simulated") : shared("card.origin.measured"),
                        shared("card.origin.label"),
                        simulated ? shared("card.simulated.pill") : shared("card.measured.pill"), FactTone.HIGHLIGHT)),
                List.of(simulated ? shared("source.virtual_node") : shared("source.nodes", nodes)), List.of()));
    }
}
