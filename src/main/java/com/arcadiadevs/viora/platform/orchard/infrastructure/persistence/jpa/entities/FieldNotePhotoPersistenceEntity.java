package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/** Bytes of the photo of a field note, keyed by the note identity. Replaced when a new photo is uploaded. */
@Entity
@Table(name = "field_note_photos")
@Getter
@Setter
public class FieldNotePhotoPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(name = "note_id", nullable = false, updatable = false)
    private UUID noteId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, length = FieldNotePhoto.MAX_BYTES)
    private byte[] content;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return noteId;
    }

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
