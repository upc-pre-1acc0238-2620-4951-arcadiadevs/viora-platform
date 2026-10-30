package com.arcadiadevs.viora.platform.assistant.application.queryservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;
import java.util.Optional;

/**
 * Application query service port for the stored answers of the assistant.
 */
public interface AssistantAnswerQueryService {

    /**
     * Lists the newest answers still kept in the history, each with its freshness.
     *
     * @param query query criteria
     * @return newest first, or application error
     */
    Result<List<AnswerView>, ApplicationError> handle(GetAnswerHistoryQuery query);

    /**
     * Reopens one answer of the producer.
     *
     * @param query query criteria
     * @return the answer, or a not found error when it is not the producer's or no longer kept
     */
    Result<AnswerView, ApplicationError> handle(GetAnswerByIdQuery query);

    /**
     * Finds the latest answer of a screen and plot.
     *
     * @param query query criteria
     * @return the answer, empty when there is none, or application error
     */
    Result<Optional<AnswerView>, ApplicationError> handle(GetLatestAnswerQuery query);
}
