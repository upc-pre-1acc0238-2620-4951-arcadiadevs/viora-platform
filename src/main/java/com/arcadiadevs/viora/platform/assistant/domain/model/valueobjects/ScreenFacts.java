package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.List;
import java.util.Optional;

/**
 * The figures of a plot one screen reads, already computed by the contexts that own them. The catalog and the answers
 * of the screen read the same record, so a question is enabled from the very figures its answer is written with.
 *
 * <p>Each screen has its own record, which hands its questions and templates to the domain class of the screen. A
 * new screen adds its record, its domain class and its reader, and no switch over the screens.</p>
 */
public sealed interface ScreenFacts permits AlternationFacts, PlanFacts {

    /**
     * The screen these figures are of.
     *
     * @return the screen
     */
    ScreenKey screen();

    /**
     * The figures the answers of the screen read, as one text: equal texts give the same data version. A figure only
     * the catalog reads stays out, so it never turns a saved answer stale.
     *
     * @return the canonical text
     */
    String canonical();

    /**
     * The questions of the screen, evaluated against these figures.
     *
     * @return the questions in display order
     */
    List<CatalogQuestion> questions();

    /**
     * Writes the answer to a question of the screen from these figures.
     *
     * @param questionId the question
     * @param plotId     the plot, for the routes of the buttons
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId);
}
