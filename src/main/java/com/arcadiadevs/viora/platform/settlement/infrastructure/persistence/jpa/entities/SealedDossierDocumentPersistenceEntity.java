package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

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

/** Exact bytes of a dossier sealed by the producer, keyed by the dossier identity. Insert only. */
@Entity
@Table(name = "sealed_dossier_documents")
@Getter
@Setter
public class SealedDossierDocumentPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(name = "dossier_id", nullable = false, updatable = false)
    private UUID dossierId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, updatable = false,
            length = DossierDocumentPersistenceEntity.CONTENT_COLUMN_LENGTH)
    private byte[] content;

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return dossierId;
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
