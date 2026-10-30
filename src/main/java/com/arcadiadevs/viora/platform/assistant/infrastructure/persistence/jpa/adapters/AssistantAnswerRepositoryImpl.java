package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerPersistenceEntity;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories.AssistantAnswerPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter of the saved answers of the assistant. */
@Repository
public class AssistantAnswerRepositoryImpl implements AssistantAnswerRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AssistantAnswerPersistenceRepository answers;

    /**
     * Creates the adapter.
     *
     * @param answers the answers table
     */
    public AssistantAnswerRepositoryImpl(AssistantAnswerPersistenceRepository answers) {
        this.answers = answers;
    }

    @Override
    public void save(AssistantAnswer answer) {
        answers.saveAndFlush(toEntity(answer));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssistantAnswer> findByIdAndProducerId(AnswerId answerId, String producerId) {
        return answers.findByIdAndProducerId(UUID.fromString(answerId.value()), producerId)
                .map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssistantAnswer> findByClientRequestId(String producerId, ClientRequestId clientRequestId) {
        return answers.findByProducerIdAndClientRequestId(producerId, clientRequestId.value())
                .map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssistantAnswer> findReusable(String producerId, PlotId plotId, QuestionId questionId,
                                                  AnswerLanguage language, DataVersion dataVersion, Instant since) {
        return answers.findReusable(producerId, plotId.plotId(), questionId.value(), language.code(),
                        dataVersion.value(), since, PageRequest.of(0, 1))
                .stream().findFirst().map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssistantAnswer> findRecent(String producerId, Instant since, int limit) {
        return answers.findByProducerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(producerId, since,
                        PageRequest.of(0, limit))
                .stream().map(AssistantAnswerRepositoryImpl::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssistantAnswer> findLatestOfScreen(String producerId, PlotId plotId, String questionPrefix,
                                                        Instant since) {
        return answers.findLatestOfScreen(producerId, plotId.plotId(), questionPrefix, since, PageRequest.of(0, 1))
                .stream().findFirst().map(AssistantAnswerRepositoryImpl::toDomain);
    }

    @Override
    @Transactional
    public void deleteAllByProducerId(String producerId) {
        answers.deleteAllByProducerId(producerId);
    }

    private static AssistantAnswerPersistenceEntity toEntity(AssistantAnswer answer) {
        var entity = new AssistantAnswerPersistenceEntity();
        entity.setId(UUID.fromString(answer.id().value()));
        entity.setProducerId(answer.producerId());
        entity.setPlotId(answer.plotId().plotId());
        entity.setQuestionId(answer.questionId().value());
        entity.setLanguage(answer.language().code());
        entity.setDataVersion(answer.dataVersion().value());
        entity.setClientRequestId(answer.clientRequestId().value());
        entity.setText(answer.text());
        entity.setFactsJson(JSON.writeValueAsString(answer.facts()));
        entity.setSourcesJson(JSON.writeValueAsString(answer.sources()));
        entity.setActionsJson(JSON.writeValueAsString(answer.actions()));
        entity.setProvider(answer.provider());
        entity.setModelName(answer.modelName());
        entity.setFallback(answer.fallback());
        entity.setInputTokens(answer.inputTokens());
        entity.setOutputTokens(answer.outputTokens());
        entity.setCreatedAt(answer.createdAt());
        return entity;
    }

    private static AssistantAnswer toDomain(AssistantAnswerPersistenceEntity entity) {
        return new AssistantAnswer(
                new AnswerId(entity.getId().toString()),
                entity.getProducerId(),
                new PlotId(entity.getPlotId()),
                new QuestionId(entity.getQuestionId()),
                new AnswerLanguage(entity.getLanguage()),
                new DataVersion(entity.getDataVersion()),
                entity.getText(),
                JSON.readValue(entity.getFactsJson(), new TypeReference<List<AnswerFact>>() { }),
                JSON.readValue(entity.getSourcesJson(), new TypeReference<List<AnswerSource>>() { }),
                JSON.readValue(entity.getActionsJson(), new TypeReference<List<AnswerAction>>() { }),
                new ClientRequestId(entity.getClientRequestId()),
                entity.getProvider(),
                entity.getModelName(),
                entity.isFallback(),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getCreatedAt());
    }
}
