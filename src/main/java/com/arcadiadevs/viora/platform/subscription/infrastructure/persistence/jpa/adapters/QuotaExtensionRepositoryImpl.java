package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.QuotaExtensionPersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.QuotaExtensionPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link QuotaExtensionRepository} with Spring Data JPA.
 */
@Repository
public class QuotaExtensionRepositoryImpl implements QuotaExtensionRepository {

    private final QuotaExtensionPersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public QuotaExtensionRepositoryImpl(QuotaExtensionPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public QuotaExtension save(QuotaExtension extension) {
        var id = UUID.fromString(extension.snapshot().id().quotaExtensionId());
        var entity = persistenceRepository.findById(id).orElseGet(QuotaExtensionPersistenceEntity::new);
        var saved = persistenceRepository.saveAndFlush(
                QuotaExtensionPersistenceAssembler.toPersistenceFromDomain(entity, extension));
        return QuotaExtensionPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<QuotaExtension> findById(QuotaExtensionId id) {
        return persistenceRepository.findById(UUID.fromString(id.quotaExtensionId()))
                .map(QuotaExtensionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByExtensionCodeIdAndProducerId(ExtensionCodeId codeId, ProducerId producerId) {
        return persistenceRepository.existsByExtensionCodeIdAndProducerId(
                UUID.fromString(codeId.extensionCodeId()), UUID.fromString(producerId.producerId()));
    }
}
