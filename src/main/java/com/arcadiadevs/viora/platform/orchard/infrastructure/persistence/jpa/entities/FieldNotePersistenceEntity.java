package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A field note of the logbook. Only {@code photo_media_type} changes after insert; the photo bytes live in
 * {@code field_note_photos}.
 */
@Entity
@Table(name = "field_notes", indexes = @Index(name = "ix_field_note_plot", columnList = "plot_id"))
@Getter
@Setter
public class FieldNotePersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plot_id", nullable = false, updatable = false, length = 36)
    private String plotId;

    @Column(name = "producer_id", nullable = false, updatable = false, length = 36)
    private String producerId;

    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private String type;

    @Column(name = "text", nullable = false, updatable = false, length = 500)
    private String text;

    @Column(name = "observed_on", nullable = false, updatable = false)
    private LocalDate observedOn;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "photo_media_type", length = 20)
    private String photoMediaType;

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
