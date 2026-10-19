package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSessionSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserSessionId;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserSessionPersistenceEntity;

import java.util.UUID;

/**
 * Unified assembler translating between the UserSession domain aggregate and its JPA persistence entity.
 */
public final class UserSessionPersistenceAssembler {

    private UserSessionPersistenceAssembler() {
    }

    /**
     * Converts a domain UserSession aggregate into a JPA entity.
     *
     * @param session domain aggregate
     * @return JPA persistence entity
     */
    public static UserSessionPersistenceEntity toPersistenceFromDomain(UserSession session) {
        var snapshot = session.snapshot();
        var entity = new UserSessionPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().sessionId()));
        entity.setUserId(UUID.fromString(snapshot.userId().userId()));
        entity.setRefreshTokenHash(snapshot.refreshTokenHash().value());
        entity.setCredentialsVersion(snapshot.credentialsVersion());
        entity.setExpiresAt(snapshot.expiresAt());
        entity.setRevokedAt(snapshot.revokedAt());
        entity.setCreatedAt(snapshot.createdAt());
        entity.setUpdatedAt(snapshot.revokedAt() != null ? snapshot.revokedAt() : snapshot.createdAt());
        return entity;
    }

    /**
     * Reconstitutes a domain UserSession aggregate from a JPA entity.
     *
     * @param entity JPA persistence entity
     * @return domain aggregate root
     */
    public static UserSession toDomainFromPersistence(UserSessionPersistenceEntity entity) {
        return UserSession.reconstitute(new UserSessionSnapshot(
                new UserSessionId(entity.getId().toString()),
                new UserId(entity.getUserId().toString()),
                new TokenHash(entity.getRefreshTokenHash()),
                entity.getCredentialsVersion(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt()
        ));
    }
}
