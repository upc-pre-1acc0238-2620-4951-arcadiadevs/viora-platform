package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

/** Counts how each question was answered, for the metrics of the assistant. Never carries producer or plot ids. */
public interface AnswerMetrics {

    /** How a question ended. */
    enum Outcome {
        /** A new answer was written. */
        CREATED,
        /** An answer with the same facts was served from the cache. */
        CACHE,
        /** The same {@code clientRequestId} returned its stored answer. */
        REPLAY,
        /** The question is disabled for the plot (409). */
        UNAVAILABLE,
        /** The producer asked too often (429). */
        RATE_LIMITED
    }

    /**
     * Counts one question.
     *
     * @param questionId the question
     * @param outcome    how it ended
     */
    void answer(QuestionId questionId, Outcome outcome);
}
