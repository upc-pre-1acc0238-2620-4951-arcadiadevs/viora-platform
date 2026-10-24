package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtensionCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.QuotaExtensionCodePersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionCodePersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.QuotaExtensionCodePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link QuotaExtensionCodeRepository} with Spring Data JPA.
 */
@Repository
public class QuotaExtensionCodeRepositoryImpl implements QuotaExtensionCodeRepository {

    private final QuotaExtensionCodePersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public QuotaExtensionCodeRepositoryImpl(QuotaExtensionCodePersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<QuotaExtensionCode> findByFingerprint(CodeFingerprint fingerprint) {
        return persistenceRepository.findByCodeHash(fingerprint.value())
                .map(QuotaExtensionCodePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public QuotaExtensionCode save(QuotaExtensionCode code) {
        var id = UUID.fromString(code.snapshot().id().extensionCodeId());
        var entity = persistenceRepository.findById(id).orElseGet(QuotaExtensionCodePersistenceEntity::new);
        var saved = persistenceRepository.saveAndFlush(
                QuotaExtensionCodePersistenceAssembler.toPersistenceFromDomain(entity, code));
        return QuotaExtensionCodePersistenceAssembler.toDomainFromPersistence(saved);
    }
}
