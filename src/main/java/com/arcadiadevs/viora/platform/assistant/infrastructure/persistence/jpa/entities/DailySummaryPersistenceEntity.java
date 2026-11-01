package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A daily summary of the home screen, saved as it was written. It never changes after insert. The cards and the
 * sources are small lists kept as JSON text: they are only ever read together with the summary. An existing
 * PostgreSQL database may create the table with scripts/sql/2026-10-31-assistant-daily-summaries.sql.
 */
@Entity
@Table(
        name = "assistant_daily_summaries",
        indexes = {
                @Index(name = "ix_assistant_daily_summary_current", columnList = "producer_id, plot_id, created_at"),
                @Index(name = "ix_assistant_daily_summary_created", columnList = "created_at")
        }
)
@Getter
@Setter
public class DailySummaryPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "producer_id", nullable = false, updatable = false, length = 36)
    private String producerId;

    /** Plot of the summary; null for the producer's whole field. */
    @Column(name = "plot_id", updatable = false, length = 36)
    private String plotId;

    @Column(name = "language", nullable = false, updatable = false, length = 2)
    private String language;

    @Column(name = "data_version", nullable = false, updatable = false, length = 80)
    private String dataVersion;

    @Column(name = "text", nullable = false, updatable = false, length = 1000)
    private String text;

    @Column(name = "facts_json", nullable = false, updatable = false, length = 2000)
    private String factsJson;

    @Column(name = "sources_json", nullable = false, updatable = false, length = 1000)
    private String sourcesJson;

    @Column(name = "provider", nullable = false, updatable = false, length = 20)
    private String provider;

    @Column(name = "model_name", updatable = false, length = 80)
    private String modelName;

    @Column(name = "fallback", nullable = false, updatable = false)
    private boolean fallback;

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
