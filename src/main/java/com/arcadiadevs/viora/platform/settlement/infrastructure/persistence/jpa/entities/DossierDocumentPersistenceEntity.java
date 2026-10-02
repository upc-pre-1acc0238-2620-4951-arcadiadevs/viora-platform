package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Exact PDF bytes of one certification, kept apart from the report tree so that loading a report never loads a
 * document. The primary key is the certification identifier assigned by the domain (a logical reference to
 * {@code dossier_certifications}); every column is non-updatable, so the stored bytes cannot change.
 */
@Entity
@Table(name = "dossier_documents")
@Getter
@Setter
public class DossierDocumentPersistenceEntity {
    /** Column length of the stored PDF: 10 MiB, the domain maximum. */
    public static final int CONTENT_COLUMN_LENGTH = 10 * 1024 * 1024;

    @Id
    @Column(name = "certification_id", nullable = false, updatable = false)
    private UUID certificationId;

    /** PDF bytes as a plain binary column (bytea on PostgreSQL), not a large object. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, updatable = false, length = CONTENT_COLUMN_LENGTH)
    private byte[] content;
}
