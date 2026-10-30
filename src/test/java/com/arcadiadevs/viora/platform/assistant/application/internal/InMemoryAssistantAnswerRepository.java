package com.arcadiadevs.viora.platform.assistant.application.internal;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository of the service tests, with the same rules as the JPA adapter (unique key per producer). */
public class InMemoryAssistantAnswerRepository implements AssistantAnswerRepository {

    private final List<AssistantAnswer> answers = new ArrayList<>();
    private final java.util.Map<String, UUID> boundRequests = new java.util.HashMap<>();

    public List<AssistantAnswer> all() {
        return List.copyOf(answers);
    }

    @Override
    public AssistantAnswer save(AssistantAnswer answer) {
        if (findByClientRequestId(answer.producerId(), answer.clientRequestId()).isPresent()) {
            throw new DataIntegrityViolationException("uk_assistant_answer_client_request");
        }
        answers.add(answer);
        return answer;
    }

    @Override
    public Optional<AssistantAnswer> findById(UUID answerId) {
        return answers.stream().filter(answer -> answer.id().equals(answerId)).findFirst();
    }

    @Override
    public Optional<AssistantAnswer> findByClientRequestId(String producerId, UUID clientRequestId) {
        var own = answers.stream()
                .filter(answer -> answer.producerId().equals(producerId)
                        && answer.clientRequestId().equals(clientRequestId))
                .findFirst();
        if (own.isPresent()) {
            return own;
        }
        return java.util.Optional.ofNullable(boundRequests.get(producerId + "/" + clientRequestId))
                .flatMap(this::findById);
    }

    @Override
    public void bindRequest(String producerId, UUID clientRequestId, UUID answerId) {
        if (findByClientRequestId(producerId, clientRequestId).isPresent()) {
            throw new DataIntegrityViolationException("uk_assistant_answer_request");
        }
        boundRequests.put(producerId + "/" + clientRequestId, answerId);
    }

    @Override
    public Optional<AssistantAnswer> findLatestCached(String producerId, QuestionId questionId, PlotId plotId,
            String locale, DataVersion dataVersion, Instant since) {
        return newestFirst().filter(answer -> answer.producerId().equals(producerId)
                        && answer.questionId().equals(questionId) && answer.plotId().equals(plotId)
                        && answer.locale().equals(locale) && answer.dataVersion().equals(dataVersion)
                        && answer.createdAt().isAfter(since))
                .findFirst();
    }

    @Override
    public List<AssistantAnswer> findRecent(String producerId, Instant since, int limit) {
        return newestFirst().filter(answer -> answer.producerId().equals(producerId)
                        && !answer.createdAt().isBefore(since))
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<AssistantAnswer> findLatestOfScreen(String producerId, PlotId plotId, String questionPrefix,
            Instant since) {
        return newestFirst().filter(answer -> answer.producerId().equals(producerId)
                        && answer.plotId().equals(plotId) && answer.questionId().value().startsWith(questionPrefix)
                        && !answer.createdAt().isBefore(since))
                .findFirst();
    }

    @Override
    public void deleteAllByProducerId(String producerId) {
        answers.removeIf(answer -> answer.producerId().equals(producerId));
        boundRequests.keySet().removeIf(key -> key.startsWith(producerId + "/"));
    }

    private java.util.stream.Stream<AssistantAnswer> newestFirst() {
        return answers.stream().sorted(Comparator.comparing(AssistantAnswer::createdAt).reversed());
    }
}
