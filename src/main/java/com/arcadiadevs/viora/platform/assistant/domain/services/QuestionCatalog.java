package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;

import java.util.List;

/**
 * Owner of the closed question catalog: which questions each screen offers, in which order, with which icon,
 * and whether the data of the plot is enough to answer them. A question without enough data is offered disabled
 * with the reason, so the model is never asked to invent a figure. Each screen keeps its questions in its own
 * domain class.
 */
public final class QuestionCatalog {

    private QuestionCatalog() {
    }

    /**
     * Evaluates the questions of a screen against the data of the plot.
     *
     * @param facts the figures of the plot the screen reads
     * @return the questions in display order
     */
    public static List<CatalogQuestion> questionsFor(ScreenFacts facts) {
        return facts.questions();
    }
}
