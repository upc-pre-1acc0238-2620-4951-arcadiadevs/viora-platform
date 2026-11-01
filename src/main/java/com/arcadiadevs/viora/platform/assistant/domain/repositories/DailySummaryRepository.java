package com.arcadiadevs.viora.platform.assistant.domain.repositories;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;

import java.time.Instant;
import java.util.Optional;

/**
 * Port of the stored daily summaries of the home screen.
 */
public interface DailySummaryRepository {

    /**
     * Saves a new summary.
     *
     * @param summary the summary
     */
    void save(DailySummary summary);

    /**
     * Finds the newest summary of a producer's plot (or whole field) written from the same figures and language, not
     * older than a moment.
     *
     * @param producerId  producer it is for
     * @param plotId      the plot; null for the whole field, which never matches a summary of a plot
     * @param language    language of the text
     * @param dataVersion fingerprint of the figures
     * @param since       oldest creation time accepted
     * @return the summary, or empty
     */
    Optional<DailySummary> findCurrent(String producerId, PlotId plotId, AnswerLanguage language,
                                       DataVersion dataVersion, Instant since);

    /**
     * Deletes the summaries of a producer.
     *
     * @param producerId producer whose account is deleted
     */
    void deleteAllByProducerId(String producerId);

    /**
     * Deletes the old summaries.
     *
     * @param before summaries created before this instant are deleted
     * @return how many were deleted
     */
    int deleteCreatedBefore(Instant before);
}
