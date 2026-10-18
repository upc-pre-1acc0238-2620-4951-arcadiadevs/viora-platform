package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA persistence entity mapping the {@code user_accounts} relational table.
 */
@Entity
@Table(name = "user_accounts", indexes = @Index(name = "idx_user_accounts_reset_token_hash", columnList = "reset_token_hash"))
@Getter
@Setter
@NoArgsConstructor
public class UserAccountPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Hashes of the previous passwords, newest first. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_password_history", joinColumns = @JoinColumn(name = "user_account_id"))
    @OrderColumn(name = "position")
    @Column(name = "password_hash", nullable = false)
    private List<String> passwordHistory = new ArrayList<>();

    /** Null for the rows created before credentials were versioned: those accounts are read as version 0. */
    @Column(name = "credentials_version")
    private Long credentialsVersion;

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

    /** SHA-256 hash of the token carried by the active password reset link; looked up when the link is used. */
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;
}
