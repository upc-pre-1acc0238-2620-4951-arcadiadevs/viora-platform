package com.arcadiadevs.viora.platform.assistant.domain.model.aggregates;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An answer of the assistant to a closed question about a plot, kept so the producer can reopen it. It is never
 * changed once stored: asking again with new data creates a new answer.
 *
 * @param id              answer identifier
 * @param clientRequestId idempotency key sent by the app, unique per producer
 * @param producerId      producer who asked
 * @param plotId          plot the question is about
 * @param questionId      question answered
 * @param locale          language of the texts ({@code es} or {@code en})
 * @param dataVersion     version of the facts the answer was built from
 * @param text            answer text
 * @param facts           exactly two fact cards
 * @param sources         what the answer is based on
 * @param actions         links to existing screens
 * @param model           what wrote the text
 * @param inputTokens     tokens sent to the model; null for templates
 * @param outputTokens    tokens received from the model; null for templates
 * @param createdAt       when it was created
 */
public record AssistantAnswer(
        UUID id,
        UUID clientRequestId,
        String producerId,
        PlotId plotId,
        QuestionId questionId,
        String locale,
        DataVersion dataVersion,
        String text,
        List<AnswerFact> facts,
        List<AnswerSource> sources,
        List<AnswerAction> actions,
        AnswerModel model,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt
) {

    /** How long an answer stays in the history. */
    public static final Duration RETENTION = Duration.ofDays(10);

    /** How many answers the history shows. */
    public static final int HISTORY_SIZE = 10;

    public AssistantAnswer {
        if (id == null || clientRequestId == null || producerId == null || producerId.isBlank() || plotId == null
                || questionId == null || locale == null || dataVersion == null || model == null
                || createdAt == null) {
            throw new IllegalArgumentException("assistant.answer.invalid");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("assistant.answer.text");
        }
        if (facts == null || facts.size() != 2) {
            throw new IllegalArgumentException("assistant.answer.facts");
        }
        facts = List.copyOf(facts);
        sources = sources == null ? List.of() : List.copyOf(sources);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /**
     * Creates a new answer.
     *
     * @param clientRequestId idempotency key sent by the app
     * @param producerId      producer who asked
     * @param plotId          plot the question is about
     * @param questionId      question answered
     * @param locale          language of the texts
     * @param dataVersion     version of the facts
     * @param text            answer text
     * @param facts           exactly two fact cards
     * @param sources         what the answer is based on
     * @param actions         links to existing screens
     * @param model           what wrote the text
     * @param now             current instant
     * @return the answer, with a new identifier and no token counts
     */
    public static AssistantAnswer create(UUID clientRequestId, String producerId, PlotId plotId,
            QuestionId questionId, String locale, DataVersion dataVersion, String text, List<AnswerFact> facts,
            List<AnswerSource> sources, List<AnswerAction> actions, AnswerModel model, Instant now) {
        return create(clientRequestId, producerId, plotId, questionId, locale, dataVersion, text, facts, sources,
                actions, model, null, null, now);
    }

    /**
     * Creates a new answer whose text a model wrote, or tried to write.
     *
     * @param clientRequestId idempotency key sent by the app
     * @param producerId      producer who asked
     * @param plotId          plot the question is about
     * @param questionId      question answered
     * @param locale          language of the texts
     * @param dataVersion     version of the facts
     * @param text            answer text
     * @param facts           exactly two fact cards
     * @param sources         what the answer is based on
     * @param actions         links to existing screens
     * @param model           what wrote the text
     * @param inputTokens     tokens sent to the model; null when none was called or it did not say
     * @param outputTokens    tokens received from the model; null when none was called or it did not say
     * @param now             current instant
     * @return the answer, with a new identifier
     */
    public static AssistantAnswer create(UUID clientRequestId, String producerId, PlotId plotId,
            QuestionId questionId, String locale, DataVersion dataVersion, String text, List<AnswerFact> facts,
            List<AnswerSource> sources, List<AnswerAction> actions, AnswerModel model, Integer inputTokens,
            Integer outputTokens, Instant now) {
        return new AssistantAnswer(UUID.randomUUID(), clientRequestId, producerId, plotId, questionId, locale,
                dataVersion, text, facts, sources, actions, model, inputTokens, outputTokens, now);
    }

    /**
     * Compares the answer with the current version of its facts.
     *
     * @param current the current version, or empty when the question cannot be answered anymore
     * @return up to date while the version is the same, stale otherwise
     */
    public Freshness freshnessAgainst(Optional<DataVersion> current) {
        return current.filter(dataVersion::equals).isPresent() ? Freshness.UP_TO_DATE : Freshness.STALE;
    }

    /**
     * Whether the answer is still in the history.
     *
     * @param now current instant
     * @return true while it is at most {@link #RETENTION} old
     */
    public boolean isRetainedAt(Instant now) {
        return !createdAt.isBefore(now.minus(RETENTION));
    }

    /**
     * Whether the answer can be served again instead of building a new one.
     *
     * @param now current instant
     * @param ttl how long an answer is served from cache
     * @return true while it is younger than the TTL
     */
    public boolean isCachedAt(Instant now, Duration ttl) {
        return createdAt.isAfter(now.minus(ttl));
    }
}
