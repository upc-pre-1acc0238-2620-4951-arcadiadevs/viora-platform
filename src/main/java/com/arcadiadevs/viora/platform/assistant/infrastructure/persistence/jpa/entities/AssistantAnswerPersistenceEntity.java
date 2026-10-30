package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored answer of the assistant. Rows are only inserted and deleted; the cards, sources and actions are kept as
 * JSON because they are always read with their answer.
 */
@Entity
@Table(name = "assistant_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_assistant_answer_client_request",
                columnNames = {"producer_id", "client_request_id"}),
        indexes = {
                @Index(name = "ix_assistant_answer_producer_created", columnList = "producer_id, created_at"),
                @Index(name = "ix_assistant_answer_cache",
                        columnList = "producer_id, question_id, plot_id, locale, data_version")
        })
@Getter
@Setter
public class AssistantAnswerPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "client_request_id", nullable = false, updatable = false)
    private UUID clientRequestId;

    @Column(name = "producer_id", nullable = false, updatable = false, length = 36)
    private String producerId;

    @Column(name = "plot_id", nullable = false, updatable = false, length = 36)
    private String plotId;

    @Column(name = "question_id", nullable = false, updatable = false, length = 60)
    private String questionId;

    @Column(name = "locale", nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(name = "data_version", nullable = false, updatable = false, length = 80)
    private String dataVersion;

    @Column(name = "text", nullable = false, updatable = false, length = 1000)
    private String text;

    @Column(name = "facts_json", nullable = false, updatable = false, length = 2000)
    private String factsJson;

    @Column(name = "sources_json", nullable = false, updatable = false, length = 1000)
    private String sourcesJson;

    @Column(name = "actions_json", nullable = false, updatable = false, length = 1000)
    private String actionsJson;

    @Column(name = "fallback", nullable = false, updatable = false)
    private boolean fallback;

    @Column(name = "model_provider", nullable = false, updatable = false, length = 40)
    private String modelProvider;

    @Column(name = "model_name", nullable = false, updatable = false, length = 80)
    private String modelName;

    @Column(name = "input_tokens", updatable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", updatable = false)
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
