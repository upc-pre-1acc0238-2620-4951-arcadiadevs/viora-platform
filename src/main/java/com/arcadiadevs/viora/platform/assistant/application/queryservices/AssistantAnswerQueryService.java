package com.arcadiadevs.viora.platform.assistant.application.queryservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;

/**
 * Application query service port for the answers the assistant already gave. Like asking, reading them needs an
 * active plan.
 */
public interface AssistantAnswerQueryService {

    /**
     * The newest answers of the producer that have not expired.
     *
     * @param query criteria
     * @return the answers, newest first, each with its freshness
     */
    Result<List<AnswerView>, ApplicationError> handle(GetAnswerHistoryQuery query);

    /**
     * One answer of the producer.
     *
     * @param query criteria
     * @return the answer, or not found when it does not exist or is another producer's
     */
    Result<AnswerView, ApplicationError> handle(GetAnswerByIdQuery query);

    /**
     * The last answer given on a screen about a plot.
     *
     * @param query criteria
     * @return the answer, or not found when none was given
     */
    Result<AnswerView, ApplicationError> handle(GetLatestAnswerQuery query);
}
