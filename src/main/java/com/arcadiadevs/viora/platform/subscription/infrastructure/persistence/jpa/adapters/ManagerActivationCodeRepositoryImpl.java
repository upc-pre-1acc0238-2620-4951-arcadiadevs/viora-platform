package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.ManagerActivationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.ManagerActivationCodePersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.ManagerActivationCodePersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.ManagerActivationCodePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link ManagerActivationCodeRepository} with Spring Data JPA.
 */
@Repository
public class ManagerActivationCodeRepositoryImpl implements ManagerActivationCodeRepository {

    private final ManagerActivationCodePersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public ManagerActivationCodeRepositoryImpl(ManagerActivationCodePersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<ManagerActivationCode> findByFingerprintForRedemption(CodeFingerprint fingerprint) {
        return persistenceRepository.findLockedByCodeHash(fingerprint.value())
                .map(ManagerActivationCodePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByFingerprint(CodeFingerprint fingerprint) {
        return persistenceRepository.existsByCodeHash(fingerprint.value());
    }

    @Override
    public ManagerActivationCode save(ManagerActivationCode code) {
        var id = UUID.fromString(code.snapshot().id().managerCodeId());
        var entity = persistenceRepository.findById(id).orElseGet(ManagerActivationCodePersistenceEntity::new);
        var saved = persistenceRepository.saveAndFlush(
                ManagerActivationCodePersistenceAssembler.toPersistenceFromDomain(entity, code));
        return ManagerActivationCodePersistenceAssembler.toDomainFromPersistence(saved);
    }
}
