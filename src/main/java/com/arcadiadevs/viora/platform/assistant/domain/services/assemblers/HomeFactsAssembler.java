package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Base of the HOME questions, about one plot or the producer's whole field. Every answer says which (fact
 * {@code scope}); a field answer also counts the active plots. A field with a single active plot reads like a plot
 * answer, since both are about the same plot.
 */
abstract class HomeFactsAssembler implements FactsAssembler {

    static final String PLOT = "PLOT";
    static final String FIELD = "FIELD";

    @Override
    public final Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var home = data.home();
        if (!home.fieldScope() && home.plots().isEmpty()) {
            // Not a HOME question: there is nothing to answer from
            return Optional.empty();
        }
        var facts = new TreeMap<String, Object>();
        facts.put("scope", home.fieldScope() ? FIELD : PLOT);
        if (home.fieldScope()) {
            facts.put("activePlots", home.activePlots());
        }
        return build(data, home, facts);
    }

    /**
     * Builds the answer of the question.
     *
     * @param data  the data of the question
     * @param home  the plot or the active plots of the producer
     * @param facts the shared facts; the question adds what it reads
     * @return the facts of the answer, or empty when a figure is missing
     */
    protected abstract Optional<AssembledFacts> build(PlotAnswerData data, HomeData home,
            SortedMap<String, Object> facts);
}
