package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.assemblers.UserAccountPersistenceAssembler;
import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories.UserAccountPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter implementing {@link UserAccountRepository} backed by Spring Data JPA.
 */
@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountPersistenceRepository persistenceRepository;

    public UserAccountRepositoryImpl(UserAccountPersistenceRepository persistenceRepository) {
        if (persistenceRepository == null) {
            throw new IllegalArgumentException("user.persistence_repository.null");
        }
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("user.aggregate.null");
        }
        var entity = UserAccountPersistenceAssembler.toPersistenceFromDomain(userAccount);
        var savedEntity = persistenceRepository.save(entity);
        return UserAccountPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        if (id == null) {
            return Optional.empty();
        }
        return persistenceRepository.findById(UUID.fromString(id.userId()))
                .map(UserAccountPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<UserAccount> findByEmail(EmailAddress email) {
        if (email == null) {
            return Optional.empty();
        }
        return persistenceRepository.findByEmail(email.value())
                .map(UserAccountPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        if (email == null) {
            return false;
        }
        return persistenceRepository.existsByEmail(email.value());
    }

    @Override
    public Optional<UserAccount> findByResetTokenHash(TokenHash resetTokenHash) {
        if (resetTokenHash == null) {
            return Optional.empty();
        }
        return persistenceRepository.findByResetTokenHash(resetTokenHash.value())
                .map(UserAccountPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<UserAccount> findPendingVerificationNotUpdatedSince(Instant cutoff) {
        if (cutoff == null) {
            return List.of();
        }
        return persistenceRepository.findByStatusAndUpdatedAtBefore(UserAccountStatus.PENDING_VERIFICATION.name(), cutoff)
                .stream()
                .map(UserAccountPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public void delete(UserAccount userAccount) {
        if (userAccount == null) {
            throw new IllegalArgumentException("user.aggregate.null");
        }
        persistenceRepository.deleteById(UUID.fromString(userAccount.snapshot().id().userId()));
    }
}
