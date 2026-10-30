package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * An idempotency key bound to an answer that another request created (a cache hit). Keys of answers created by
 * their own request live on the answer row itself.
 */
@Entity
@Table(name = "assistant_answer_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_assistant_answer_request",
                columnNames = {"producer_id", "client_request_id"}),
        indexes = @Index(name = "ix_assistant_answer_request_answer", columnList = "answer_id"))
@Getter
@Setter
public class AssistantAnswerRequestPersistenceEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "producer_id", nullable = false, updatable = false, length = 36)
    private String producerId;

    @Column(name = "client_request_id", nullable = false, updatable = false)
    private UUID clientRequestId;

    @Column(name = "answer_id", nullable = false, updatable = false)
    private UUID answerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
