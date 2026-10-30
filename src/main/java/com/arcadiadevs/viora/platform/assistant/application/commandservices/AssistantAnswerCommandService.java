package com.arcadiadevs.viora.platform.assistant.application.commandservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application command service port answering the closed questions of the assistant.
 */
public interface AssistantAnswerCommandService {

    /**
     * Outcome of a question.
     *
     * @param view    the answer with its freshness
     * @param created whether a new answer was created; false when it was served from cache or replayed
     */
    record AnswerOutcome(AnswerView view, boolean created) {
    }

    /**
     * Answers a question, serving a stored answer when the same request or the same data was already answered.
     *
     * @param command the question
     * @return the outcome or application error
     */
    Result<AnswerOutcome, ApplicationError> handle(AskQuestionCommand command);
}
