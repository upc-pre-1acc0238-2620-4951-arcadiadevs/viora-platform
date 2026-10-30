package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.Optional;

/**
 * Builds the facts of one question from the data of the plot: the numbers already computed, the template of the
 * text, exactly two fact cards, the sources and the actions. It never invents a figure: when one is missing the
 * question cannot be answered.
 */
public interface FactsAssembler {

    /**
     * The question this assembler answers.
     *
     * @return the question
     */
    QuestionId questionId();

    /**
     * Builds the facts of the question.
     *
     * @param data the data of the plot
     * @return the facts, or empty when a figure the answer needs is missing
     */
    Optional<AssembledFacts> assemble(PlotAnswerData data);
}
