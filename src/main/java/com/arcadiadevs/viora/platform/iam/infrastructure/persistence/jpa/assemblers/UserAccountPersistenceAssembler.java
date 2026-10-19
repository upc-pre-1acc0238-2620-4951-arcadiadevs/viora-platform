package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserAccountPersistenceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unified assembler translating between UserAccount domain aggregate and JPA persistence entity.
 */
public final class UserAccountPersistenceAssembler {

    private UserAccountPersistenceAssembler() {
    }

    /**
     * Converts a domain UserAccount aggregate into a JPA entity.
     *
     * @param account domain aggregate
     * @return JPA persistence entity
     */
    public static UserAccountPersistenceEntity toPersistenceFromDomain(UserAccount account) {
        if (account == null) {
            return null;
        }
        var snapshot = account.snapshot();
        var entity = new UserAccountPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().userId()));
        entity.setEmail(snapshot.email().value());
        entity.setPasswordHash(snapshot.passwordHash().value());
        entity.setPasswordHistory(new ArrayList<>(snapshot.passwordHistory().previousHashes().stream()
                .map(PasswordHash::value)
                .toList()));
        entity.setCredentialsVersion(snapshot.credentialsVersion());
        entity.setRole(snapshot.role().name());
        entity.setStatus(snapshot.status().name());

        if (snapshot.emailVerification() != null) {
            entity.setEmailVerificationCodeHash(snapshot.emailVerification().codeHash());
            entity.setEmailVerificationIssuedAt(snapshot.emailVerification().issuedAt());
            entity.setEmailVerificationFailedAttempts(snapshot.emailVerification().failedAttempts());
        } else {
            entity.setEmailVerificationCodeHash(null);
            entity.setEmailVerificationIssuedAt(null);
            entity.setEmailVerificationFailedAttempts(null);
        }

        if (snapshot.termsAcceptance() != null) {
            entity.setTermsVersion(snapshot.termsAcceptance().version());
            entity.setTermsAcceptedAt(snapshot.termsAcceptance().acceptedAt());
        } else {
            entity.setTermsVersion(null);
            entity.setTermsAcceptedAt(null);
        }

        if (snapshot.resetToken() != null) {
            entity.setResetTokenHash(snapshot.resetToken().tokenHash().value());
            entity.setResetTokenExpiresAt(snapshot.resetToken().expiresAt());
        } else {
            entity.setResetTokenHash(null);
            entity.setResetTokenExpiresAt(null);
        }

        entity.setCreatedAt(snapshot.createdAt());
        entity.setUpdatedAt(snapshot.updatedAt());
        return entity;
    }

    /**
     * Reconstitutes a domain UserAccount aggregate from a JPA entity.
     *
     * @param entity JPA persistence entity
     * @return domain aggregate root
     */
    public static UserAccount toDomainFromPersistence(UserAccountPersistenceEntity entity) {
        if (entity == null) {
            return null;
        }

        PasswordResetToken resetToken = null;
        if (entity.getResetTokenHash() != null && entity.getResetTokenExpiresAt() != null) {
            resetToken = new PasswordResetToken(new TokenHash(entity.getResetTokenHash()), entity.getResetTokenExpiresAt());
        }

        EmailVerificationToken emailVerification = null;
        if (entity.getEmailVerificationCodeHash() != null && entity.getEmailVerificationIssuedAt() != null) {
            int failedAttempts = entity.getEmailVerificationFailedAttempts() != null
                    ? entity.getEmailVerificationFailedAttempts() : 0;
            emailVerification = new EmailVerificationToken(entity.getEmailVerificationCodeHash(),
                    entity.getEmailVerificationIssuedAt(), failedAttempts);
        }

        TermsAcceptance termsAcceptance = null;
        if (entity.getTermsVersion() != null && entity.getTermsAcceptedAt() != null) {
            termsAcceptance = new TermsAcceptance(entity.getTermsVersion(), entity.getTermsAcceptedAt());
        }

        // Accounts created before the email verification existed have no status and stay active
        var status = entity.getStatus() != null ? UserAccountStatus.valueOf(entity.getStatus()) : UserAccountStatus.ACTIVE;

        var snapshot = new UserAccountSnapshot(
                new UserId(entity.getId().toString()),
                new EmailAddress(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                new PasswordHistory(entity.getPasswordHistory() == null ? List.of()
                        : entity.getPasswordHistory().stream().map(PasswordHash::new).toList()),
                entity.getCredentialsVersion() != null ? entity.getCredentialsVersion() : 0L,
                UserRole.valueOf(entity.getRole()),
                status,
                emailVerification,
                termsAcceptance,
                resetToken,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );

        return UserAccount.reconstitute(snapshot);
    }
}
