package com.arcadiadevs.viora.platform.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base abstract domain entity providing auditing metadata (identifiers and timestamps).
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditableModel {
    /**
     * The unique persistence identifier of this entity.
     */
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The date and time when this entity was created.
     */
    @Getter
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The date and time when this entity was last modified.
     */
    @Getter
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}