package com.arcadiadevs.viora.platform.assistant.application.commandservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application command service port preparing the daily summary of the home screen, precomputed once a day by a job
 * and generated on demand when it is missing or its figures changed.
 */
public interface DailySummaryCommandService {

    /**
     * The summary served and whether it was just written.
     *
     * @param summary the summary of the day
     * @param created true when it was written by this request, false when the stored one was served
     */
    record Outcome(DailySummary summary, boolean created) {
    }

    /**
     * Serves the summary of the day built from the current figures, or writes it.
     *
     * @param command the producer, the plot (or the whole field) and the language
     * @return the outcome or an application error
     */
    Result<Outcome, ApplicationError> handle(PrepareDailySummaryCommand command);
}
