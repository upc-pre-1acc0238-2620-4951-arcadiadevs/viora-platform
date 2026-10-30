package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;

/**
 * Whether a question can be answered now, with its facts, or why it cannot.
 */
public sealed interface AnswerAssessment {

    /**
     * The question can be answered.
     *
     * @param facts the current facts
     */
    record Available(AssembledFacts facts) implements AnswerAssessment {
    }

    /**
     * The question is disabled.
     *
     * @param reason what is missing, as the catalog shows it
     */
    record Unavailable(CatalogText reason) implements AnswerAssessment {
    }
}
