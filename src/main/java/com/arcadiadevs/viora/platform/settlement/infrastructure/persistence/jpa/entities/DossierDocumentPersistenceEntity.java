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

/**
 * Exact PDF bytes of one certification, kept apart from the report tree so that loading a report never loads a
 * document. The primary key is the certification identifier assigned by the domain (a logical reference to
 * {@code dossier_certifications}); every column is non-updatable, so the stored bytes cannot change.
 *
 * <p>Implements {@link Persistable} because the identifier is assigned by the domain: without it Spring Data would
 * treat every new document as existing and merge it (an extra SELECT before the INSERT). A second document for the
 * same certification is rejected by the primary key.</p>
 */
@Entity
@Table(name = "dossier_documents")
@Getter
@Setter
public class DossierDocumentPersistenceEntity implements Persistable<UUID> {
    /** Column length of the stored PDF: 10 MiB, the domain maximum. */
    public static final int CONTENT_COLUMN_LENGTH = 10 * 1024 * 1024;

    @Id
    @Column(name = "certification_id", nullable = false, updatable = false)
    private UUID certificationId;

    /** PDF bytes as a plain binary column (bytea on PostgreSQL), not a large object. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, updatable = false, length = CONTENT_COLUMN_LENGTH)
    private byte[] content;

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return certificationId;
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
