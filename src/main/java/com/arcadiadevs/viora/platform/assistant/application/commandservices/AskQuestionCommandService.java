package com.arcadiadevs.viora.platform.assistant.application.commandservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application command service port for asking the assistant a closed question.
 */
public interface AskQuestionCommandService {

    /**
     * Answers a question about a plot. The same client request id always gets the same answer, and the data of the
     * plot being unchanged gets the answer already saved unless a refresh is forced.
     *
     * @param command the question
     * @return the answer with its freshness, or an application error: 403 without an active plan or on a plot of
     *         another producer, 404 for an unknown plot, 409 for a question without enough data
     */
    Result<AnswerView, ApplicationError> handle(AskQuestionCommand command);
}
