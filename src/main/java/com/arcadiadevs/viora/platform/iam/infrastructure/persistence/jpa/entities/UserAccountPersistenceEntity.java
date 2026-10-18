package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA persistence entity mapping the {@code user_accounts} relational table.
 */
@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
public class UserAccountPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /** Null for the rows created before accounts had a status: those accounts are read as active. */
    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "email_verification_code_hash")
    private String emailVerificationCodeHash;

    @Column(name = "email_verification_issued_at")
    private Instant emailVerificationIssuedAt;

    @Column(name = "email_verification_failed_attempts")
    private Integer emailVerificationFailedAttempts;

    @Column(name = "terms_version", length = 50)
    private String termsVersion;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "reset_code_hash")
    private String resetCodeHash;

    @Column(name = "reset_failed_attempts")
    private Integer resetFailedAttempts;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;
}
