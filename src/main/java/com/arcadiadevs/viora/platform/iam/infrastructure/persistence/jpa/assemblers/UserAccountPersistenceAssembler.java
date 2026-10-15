package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserAccountPersistenceEntity;

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
        entity.setRole(snapshot.role().name());

        if (snapshot.resetToken() != null) {
            entity.setResetCodeHash(snapshot.resetToken().codeHash());
            entity.setResetTokenExpiresAt(snapshot.resetToken().expiresAt());
            entity.setResetFailedAttempts(snapshot.resetToken().failedAttempts());
        } else {
            entity.setResetCodeHash(null);
            entity.setResetTokenExpiresAt(null);
            entity.setResetFailedAttempts(null);
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
        if (entity.getResetCodeHash() != null && entity.getResetTokenExpiresAt() != null) {
            int failedAttempts = entity.getResetFailedAttempts() != null ? entity.getResetFailedAttempts() : 0;
            resetToken = new PasswordResetToken(entity.getResetCodeHash(), entity.getResetTokenExpiresAt(), failedAttempts);
        }

        var snapshot = new UserAccountSnapshot(
                new UserId(entity.getId().toString()),
                new EmailAddress(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                UserRole.valueOf(entity.getRole()),
                resetToken,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );

        return UserAccount.reconstitute(snapshot);
    }
}
