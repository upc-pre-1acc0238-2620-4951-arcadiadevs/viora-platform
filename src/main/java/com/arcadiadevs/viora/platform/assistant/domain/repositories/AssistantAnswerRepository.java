package com.arcadiadevs.viora.platform.assistant.domain.repositories;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port of the saved answers of the assistant.
 */
public interface AssistantAnswerRepository {

    /**
     * Saves a new answer.
     *
     * @param answer the answer
     * @throws org.springframework.dao.DataIntegrityViolationException if the producer already used the same
     *                                                                 client request id
     */
    void save(AssistantAnswer answer);

    /**
     * Finds an answer of a producer by its identifier.
     *
     * @param producerId producer who owns it
     * @param answerId   the answer
     * @return the answer, or empty when it does not exist or belongs to another producer
     */
    Optional<AssistantAnswer> findByIdAndProducerId(AnswerId answerId, String producerId);

    /**
     * Finds the answer a producer already got for an idempotency key.
     *
     * @param producerId      producer who asked
     * @param clientRequestId the key
     * @return the answer, or empty
     */
    Optional<AssistantAnswer> findByClientRequestId(String producerId, ClientRequestId clientRequestId);

    /**
     * Finds the newest answer to a question about a plot, written from the same data, language and not older than a
     * moment.
     *
     * @param producerId  producer who asked
     * @param plotId      the plot; null for the producer's whole field, which never matches an answer about a plot
     * @param questionId  the question
     * @param language    language of the text
     * @param dataVersion fingerprint of the data
     * @param since       oldest creation time accepted
     * @return the answer, or empty
     */
    Optional<AssistantAnswer> findReusable(String producerId, PlotId plotId, QuestionId questionId,
                                           AnswerLanguage language, DataVersion dataVersion, Instant since);

    /**
     * Lists the newest answers of a producer.
     *
     * @param producerId producer who asked
     * @param since      oldest creation time accepted
     * @param limit      how many at most
     * @return the answers, newest first
     */
    List<AssistantAnswer> findRecent(String producerId, Instant since, int limit);

    /**
     * Finds the newest answer of a producer about a plot to a question of a screen.
     *
     * @param producerId     producer who asked
     * @param plotId         the plot; null for the answers about the producer's whole field only
     * @param questionPrefix prefix of the questions of the screen, for example {@code plan.}
     * @param since          oldest creation time accepted
     * @return the answer, or empty
     */
    Optional<AssistantAnswer> findLatestOfScreen(String producerId, PlotId plotId, String questionPrefix,
                                                 Instant since);

    /**
     * Deletes the answers of a producer.
     *
     * @param producerId producer whose account is deleted
     */
    void deleteAllByProducerId(String producerId);

    /**
     * Deletes the answers that left the history.
     *
     * @param before answers created before this instant are deleted
     * @return how many were deleted
     */
    int deleteCreatedBefore(Instant before);
}
