package com.arcadiadevs.viora.platform.assistant.domain.model.commands;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;

/**
 * Command preparing the daily summary of the home screen: the stored summary of the day when its figures have not
 * changed, or a new one. There is no idempotency key: the request reads the summary of the day, and two requests
 * racing at most store two equal summaries, of which the newest is served.
 *
 * @param producerId producer whose summary it is, whose plan must be active
 * @param plotId     plot the summary is about; null for the producer's whole field
 * @param language   language of the texts
 * @param scheduled  whether the daily job prepares it: the job never spends the producer's limits
 */
public record PrepareDailySummaryCommand(String producerId, PlotId plotId, AnswerLanguage language,
                                         boolean scheduled) {

    /**
     * Compact constructor validating the command.
     */
    public PrepareDailySummaryCommand {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        language = language == null ? AnswerLanguage.SPANISH : language;
    }
}
