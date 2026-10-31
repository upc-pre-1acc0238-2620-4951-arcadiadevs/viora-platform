package com.arcadiadevs.viora.platform.assistant.domain.model.commands;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

/**
 * Command asking a closed question about a plot, or about the producer's whole field on a screen with field scope
 * (HOME).
 *
 * @param producerId      producer asking, whose plan must be active
 * @param questionId      question of the catalog
 * @param plotId          plot the question is about; null only for a question of a screen with field scope
 * @param clientRequestId idempotency key: the same key gets the same answer
 * @param forceRefresh    ask again even if a saved answer for the same data exists
 * @param language        language to answer in
 */
public record AskQuestionCommand(
        String producerId,
        QuestionId questionId,
        PlotId plotId,
        ClientRequestId clientRequestId,
        boolean forceRefresh,
        AnswerLanguage language
) {

    /**
     * Compact constructor validating that nothing is missing.
     */
    public AskQuestionCommand {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (questionId == null) {
            throw new IllegalArgumentException("assistant.question.invalid");
        }
        var screen = questionId.screen();
        if (plotId == null && screen.requiresPlot()) {
            throw new IllegalArgumentException("assistant.plot.required");
        }
        if (clientRequestId == null) {
            throw new IllegalArgumentException("assistant.client_request_id.invalid");
        }
        if (language == null) {
            throw new IllegalArgumentException("assistant.language.invalid");
        }
    }
}
