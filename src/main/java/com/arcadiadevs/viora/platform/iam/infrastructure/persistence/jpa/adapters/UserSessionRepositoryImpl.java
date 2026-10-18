package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.assemblers.UserSessionPersistenceAssembler;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories.UserSessionPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter implementing {@link UserSessionRepository} backed by Spring Data JPA.
 */
@Repository
public class UserSessionRepositoryImpl implements UserSessionRepository {

    private final UserSessionPersistenceRepository persistenceRepository;

    public UserSessionRepositoryImpl(UserSessionPersistenceRepository persistenceRepository) {
        if (persistenceRepository == null) {
            throw new IllegalArgumentException("user.session.persistence_repository.null");
        }
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public UserSession save(UserSession session) {
        if (session == null) {
            throw new IllegalArgumentException("user.session.aggregate.null");
        }
        var saved = persistenceRepository.save(UserSessionPersistenceAssembler.toPersistenceFromDomain(session));
        return UserSessionPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<UserSession> findByRefreshTokenHash(TokenHash refreshTokenHash) {
        if (refreshTokenHash == null) {
            return Optional.empty();
        }
        return persistenceRepository.findByRefreshTokenHash(refreshTokenHash.value())
                .map(UserSessionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<UserSession> findNotRevokedByUserId(UserId userId) {
        if (userId == null) {
            return List.of();
        }
        return persistenceRepository.findByUserIdAndRevokedAtIsNull(UUID.fromString(userId.userId())).stream()
                .map(UserSessionPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
