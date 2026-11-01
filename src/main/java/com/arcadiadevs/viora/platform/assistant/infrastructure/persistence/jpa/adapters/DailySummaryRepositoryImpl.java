package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.DailySummaryRepository;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.DailySummaryPersistenceEntity;
import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories.DailySummaryPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter of the stored daily summaries of the assistant. */
@Repository
public class DailySummaryRepositoryImpl implements DailySummaryRepository {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final DailySummaryPersistenceRepository summaries;

    /**
     * Creates the adapter.
     *
     * @param summaries the summaries table
     */
    public DailySummaryRepositoryImpl(DailySummaryPersistenceRepository summaries) {
        this.summaries = summaries;
    }

    @Override
    public void save(DailySummary summary) {
        summaries.saveAndFlush(toEntity(summary));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DailySummary> findCurrent(String producerId, PlotId plotId, AnswerLanguage language,
                                              DataVersion dataVersion, Instant since) {
        var page = PageRequest.of(0, 1);
        var found = plotId == null
                ? summaries.findCurrentOfField(producerId, language.code(), dataVersion.value(), since, page)
                : summaries.findCurrent(producerId, plotId.plotId(), language.code(), dataVersion.value(), since,
                        page);
        return found.stream().findFirst().map(DailySummaryRepositoryImpl::toDomain);
    }

    @Override
    @Transactional
    public void deleteAllByProducerId(String producerId) {
        summaries.deleteAllByProducerId(producerId);
    }

    @Override
    @Transactional
    public int deleteCreatedBefore(Instant before) {
        return summaries.deleteCreatedBefore(before);
    }

    private static DailySummaryPersistenceEntity toEntity(DailySummary summary) {
        var entity = new DailySummaryPersistenceEntity();
        entity.setId(UUID.fromString(summary.id().value()));
        entity.setProducerId(summary.producerId());
        entity.setPlotId(summary.plotId() == null ? null : summary.plotId().plotId());
        entity.setLanguage(summary.language().code());
        entity.setDataVersion(summary.dataVersion().value());
        entity.setText(summary.text());
        entity.setFactsJson(JSON.writeValueAsString(summary.facts()));
        entity.setSourcesJson(JSON.writeValueAsString(summary.sources()));
        entity.setProvider(summary.provider());
        entity.setModelName(summary.modelName());
        entity.setFallback(summary.fallback());
        entity.setInputTokens(summary.inputTokens());
        entity.setOutputTokens(summary.outputTokens());
        entity.setCreatedAt(summary.createdAt());
        return entity;
    }

    private static DailySummary toDomain(DailySummaryPersistenceEntity entity) {
        return new DailySummary(
                new DailySummaryId(entity.getId().toString()),
                entity.getProducerId(),
                entity.getPlotId() == null ? null : new PlotId(entity.getPlotId()),
                new AnswerLanguage(entity.getLanguage()),
                new DataVersion(entity.getDataVersion()),
                entity.getText(),
                JSON.readValue(entity.getFactsJson(), new TypeReference<List<AnswerFact>>() { }),
                JSON.readValue(entity.getSourcesJson(), new TypeReference<List<AnswerSource>>() { }),
                entity.getProvider(),
                entity.getModelName(),
                entity.isFallback(),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getCreatedAt());
    }
}
