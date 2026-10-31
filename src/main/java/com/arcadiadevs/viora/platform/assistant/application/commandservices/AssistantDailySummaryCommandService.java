package com.arcadiadevs.viora.platform.assistant.application.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application command service port preparing the daily summary of Inicio, precomputed once a day by a job and
 * generated on demand when it is missing or its data changed.
 */
public interface AssistantDailySummaryCommandService {

    /**
     * Serves the summary of the day built from the current data, or creates it.
     *
     * @param command the producer, the plot (or the whole field) and the language
     * @return the outcome (created, or served from the summaries of the day) or application error
     */
    Result<AnswerOutcome, ApplicationError> handle(PrepareDailySummaryCommand command);
}
