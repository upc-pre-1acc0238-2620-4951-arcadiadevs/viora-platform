package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo vinculo un nodo?": the nodes the plot already has and what Telemetry asks to link one (a name unique in
 * the plot, its type and, for a soil probe, its depth). No deeplink: the app has no agreed route to the node form yet.
 */
public final class SensorsHowLinkFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.SENSORS_HOW_LINK;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        if (climate.coverage() == null) {
            // Not a SENSORS question
            return Optional.empty();
        }
        int nodes = climate.nodes().size();
        int probes = (int) climate.nodes().stream().filter(node -> ClimateData.SOIL_PROBE.equals(node.type())).count();
        int microclimate = nodes - probes;
        var facts = new TreeMap<String, Object>();
        facts.put("linkedNodes", nodes);
        facts.put("microclimateNodes", microclimate);
        facts.put("soilProbes", probes);
        var state = nodes == 0 ? text(questionId(), "state.none") : text(questionId(), "state.some", nodes);
        return Optional.of(new AssembledFacts(facts, text(questionId(), "text", state, text(questionId(), "steps")),
                List.of(
                        card(shared("value.integer", microclimate), shared("card.microclimate.label", microclimate),
                                shared("card.this_plot.pill"), FactTone.PRIMARY),
                        card(shared("value.integer", probes), shared("card.soil_probes.label", probes),
                                shared("card.this_plot.pill"), FactTone.HIGHLIGHT)),
                List.of(shared("source.linked_nodes")), List.of()));
    }
}
