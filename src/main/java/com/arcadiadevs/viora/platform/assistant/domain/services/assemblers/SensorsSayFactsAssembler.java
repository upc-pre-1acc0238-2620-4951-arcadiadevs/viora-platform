package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué dicen mis sensores?": the latest reading of the plot. Unless every linked node is measured, the reading is
 * labeled "dato simulado" (contract, section 3.3). CLIMATE and SENSORS ask it with their own question and texts.
 */
public final class SensorsSayFactsAssembler implements FactsAssembler {

    /** Fact the model reads to say "dato simulado". */
    static final String SIMULATED = "simulated";

    private final QuestionId questionId;

    /** The {@code climate.sensors_say} question. */
    public SensorsSayFactsAssembler() {
        this(QuestionId.CLIMATE_SENSORS_SAY);
    }

    /**
     * The same answer for another question.
     *
     * @param questionId the question, such as {@code sensors.what_say}
     */
    public SensorsSayFactsAssembler(QuestionId questionId) {
        this.questionId = questionId;
    }

    @Override
    public QuestionId questionId() {
        return questionId;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        var reading = climate.latestReading();
        if (reading == null || climate.nodes().isEmpty()) {
            return Optional.empty();
        }
        boolean simulated = climate.nodes().stream().anyMatch(SensorNode::simulated);
        int nodes = climate.nodes().size();
        double temperature = oneDecimal(reading.temperature());
        long humidity = whole(reading.humidity());
        double soil = oneDecimal(reading.soilMoisture());

        var facts = new TreeMap<String, Object>();
        facts.put("linkedNodes", nodes);
        facts.put(SIMULATED, simulated);
        facts.put("airTemperature", temperature);
        facts.put("airHumidity", humidity);
        facts.put("soilMoisture", soil);
        var text = text(questionId(), simulated ? "text.simulated" : "text.measured", nodes, temperature, humidity,
                soil);
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.measure", temperature, "°C"), shared("card.air_temperature.label"),
                        simulated ? shared("card.simulated.pill") : shared("card.measured.pill"), FactTone.PRIMARY),
                card(shared("value.measure", soil, "%"), shared("card.soil_moisture.label"),
                        shared("card.air_humidity.pill", humidity), FactTone.HIGHLIGHT)),
                List.of(simulated ? shared("source.virtual_node") : shared("source.nodes", nodes)), List.of()));
    }
}
