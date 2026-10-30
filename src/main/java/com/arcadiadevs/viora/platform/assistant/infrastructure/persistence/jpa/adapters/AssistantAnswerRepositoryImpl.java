package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerPersistenceEntity;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerRequestPersistenceEntity;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories.AssistantAnswerPersistenceRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories.AssistantAnswerRequestPersistenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter of the assistant answers.
 *
 * <p>Every idempotency key gets a row in {@code assistant_answer_requests}, the key of a new answer too (besides the
 * column of the answer row): the unique key of that table is then the one place where a request creating an answer
 * and a request served from the cache with the same key collide, so only one of them wins.</p>
 */
@Repository
public class AssistantAnswerRepositoryImpl implements AssistantAnswerRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AssistantAnswerPersistenceRepository answers;
    private final AssistantAnswerRequestPersistenceRepository requests;
    private final Clock clock;

    public AssistantAnswerRepositoryImpl(AssistantAnswerPersistenceRepository answers,
            AssistantAnswerRequestPersistenceRepository requests, Clock clock) {
        this.answers = answers;
        this.requests = requests;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AssistantAnswer save(AssistantAnswer answer) {
        var saved = toDomain(answers.saveAndFlush(toEntity(answer)));
        // Same unique key as a key served from the cache: a race between both rolls this answer back
        insertRequest(answer.producerId(), answer.clientRequestId(), answer.id());
        return saved;
    }

    @Override
    public Optional<AssistantAnswer> findById(UUID answerId) {
        return answers.findById(answerId).map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    public Optional<AssistantAnswer> findByClientRequestId(String producerId, UUID clientRequestId) {
        var own = answers.findByProducerIdAndClientRequestId(producerId, clientRequestId);
        if (own.isPresent()) {
            return own.map(AssistantAnswerRepositoryImpl::toDomain);
        }
        return requests.findByProducerIdAndClientRequestId(producerId, clientRequestId)
                .flatMap(request -> answers.findById(request.getAnswerId()))
                .filter(answer -> answer.getProducerId().equals(producerId))
                .map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    public void bindRequest(String producerId, UUID clientRequestId, UUID answerId) {
        // Answers stored before every key had its row only carry it in their own column
        if (answers.findByProducerIdAndClientRequestId(producerId, clientRequestId).isPresent()) {
            throw new DataIntegrityViolationException("uk_assistant_answer_client_request");
        }
        insertRequest(producerId, clientRequestId, answerId);
    }

    private void insertRequest(String producerId, UUID clientRequestId, UUID answerId) {
        var request = new AssistantAnswerRequestPersistenceEntity();
        request.setId(UUID.randomUUID());
        request.setProducerId(producerId);
        request.setClientRequestId(clientRequestId);
        request.setAnswerId(answerId);
        request.setCreatedAt(clock.instant());
        requests.saveAndFlush(request);
    }

    @Override
    public Optional<AssistantAnswer> findLatestCached(String producerId, QuestionId questionId, PlotId plotId,
            String locale, DataVersion dataVersion, Instant since) {
        return answers
                .findFirstByProducerIdAndQuestionIdAndPlotIdAndLocaleAndDataVersionAndCreatedAtGreaterThanOrderByCreatedAtDesc(
                        producerId, questionId.value(), plotId.plotId(), locale, dataVersion.value(), since)
                .map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    public List<AssistantAnswer> findRecent(String producerId, Instant since, int limit) {
        return answers.findByProducerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(producerId, since,
                        PageRequest.of(0, limit)).stream()
                .map(AssistantAnswerRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<AssistantAnswer> findLatestOfScreen(String producerId, PlotId plotId, String questionPrefix,
            Instant since) {
        return answers
                .findFirstByProducerIdAndPlotIdAndQuestionIdStartingWithAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        producerId, plotId.plotId(), questionPrefix, since)
                .map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    @Transactional
    public void deleteAllByProducerId(String producerId) {
        requests.deleteAllByProducerIdInBulk(producerId);
        answers.deleteAllByProducerIdInBulk(producerId);
    }

    @Override
    @Transactional
    public int deleteCreatedBefore(Instant cutoff) {
        requests.deleteExpired(cutoff);
        return answers.deleteCreatedBefore(cutoff);
    }

    private static AssistantAnswerPersistenceEntity toEntity(AssistantAnswer answer) {
        var entity = new AssistantAnswerPersistenceEntity();
        entity.setId(answer.id());
        entity.setClientRequestId(answer.clientRequestId());
        entity.setProducerId(answer.producerId());
        entity.setPlotId(answer.plotId().plotId());
        entity.setQuestionId(answer.questionId().value());
        entity.setLocale(answer.locale());
        entity.setDataVersion(answer.dataVersion().value());
        entity.setText(answer.text());
        entity.setFactsJson(JSON.writeValueAsString(answer.facts()));
        entity.setSourcesJson(JSON.writeValueAsString(answer.sources()));
        entity.setActionsJson(JSON.writeValueAsString(answer.actions()));
        entity.setFallback(answer.model().fallback());
        entity.setModelProvider(answer.model().provider());
        entity.setModelName(answer.model().name());
        entity.setInputTokens(answer.inputTokens());
        entity.setOutputTokens(answer.outputTokens());
        entity.setCreatedAt(answer.createdAt());
        return entity;
    }

    private static AssistantAnswer toDomain(AssistantAnswerPersistenceEntity entity) {
        return new AssistantAnswer(
                entity.getId(),
                entity.getClientRequestId(),
                entity.getProducerId(),
                new PlotId(entity.getPlotId()),
                new QuestionId(entity.getQuestionId()),
                entity.getLocale(),
                new DataVersion(entity.getDataVersion()),
                entity.getText(),
                JSON.readValue(entity.getFactsJson(), new TypeReference<List<AnswerFact>>() { }),
                JSON.readValue(entity.getSourcesJson(), new TypeReference<List<AnswerSource>>() { }),
                JSON.readValue(entity.getActionsJson(), new TypeReference<List<AnswerAction>>() { }),
                new AnswerModel(entity.getModelProvider(), entity.getModelName(), entity.isFallback()),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getCreatedAt());
    }
}
