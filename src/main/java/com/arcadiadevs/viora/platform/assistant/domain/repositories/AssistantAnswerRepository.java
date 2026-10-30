package com.arcadiadevs.viora.platform.assistant.domain.repositories;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port of the assistant answers.
 */
public interface AssistantAnswerRepository {

    /**
     * Stores a new answer.
     *
     * @param answer the answer
     * @return the stored answer
     * @throws org.springframework.dao.DataIntegrityViolationException when the producer already used its
     *         client request id, for an answer of its own or one served from the cache
     */
    AssistantAnswer save(AssistantAnswer answer);

    Optional<AssistantAnswer> findById(UUID answerId);

    /**
     * Finds the answer a producer created with an idempotency key.
     *
     * @param producerId      the producer
     * @param clientRequestId the key sent by the app
     * @return the answer, if that key was used
     */
    Optional<AssistantAnswer> findByClientRequestId(String producerId, UUID clientRequestId);

    /**
     * Binds an idempotency key to an answer that another request created, such as one served from the cache, so a
     * retry with that key returns the same answer.
     *
     * @param producerId      the producer
     * @param clientRequestId the key sent by the app
     * @param answerId        the answer served for that key
     * @throws org.springframework.dao.DataIntegrityViolationException when the key is already bound or used
     */
    void bindRequest(String producerId, UUID clientRequestId, UUID answerId);

    /**
     * Finds the newest answer built from the same facts, in the same language, created since an instant.
     *
     * @param producerId  the producer
     * @param questionId  the question
     * @param plotId      the plot; null for an answer about the whole field, which never matches a plot one
     * @param locale      the language
     * @param dataVersion the version of the facts
     * @param since       oldest creation instant accepted
     * @return the newest matching answer
     */
    Optional<AssistantAnswer> findLatestCached(String producerId, QuestionId questionId, PlotId plotId,
            String locale, DataVersion dataVersion, Instant since);

    /**
     * Lists the newest answers of a producer created since an instant.
     *
     * @param producerId the producer
     * @param since      oldest creation instant accepted
     * @param limit      maximum answers
     * @return newest first
     */
    List<AssistantAnswer> findRecent(String producerId, Instant since, int limit);

    /**
     * Finds the newest answer of a producer about a plot among the questions of a screen.
     *
     * @param producerId     the producer
     * @param plotId         the plot; null for the answers about the whole field
     * @param questionPrefix prefix of the screen questions, such as {@code plan.}
     * @param since          oldest creation instant accepted
     * @return the newest matching answer
     */
    Optional<AssistantAnswer> findLatestOfScreen(String producerId, PlotId plotId, String questionPrefix,
            Instant since);

    /**
     * Removes every answer of a producer and every idempotency key it used, when the account is deleted.
     *
     * @param producerId the producer
     */
    void deleteAllByProducerId(String producerId);

    /**
     * Removes the answers created before an instant, the keys bound to them and the keys created before it.
     *
     * @param cutoff the oldest creation instant kept
     * @return the answers removed
     */
    int deleteCreatedBefore(Instant cutoff);
}
