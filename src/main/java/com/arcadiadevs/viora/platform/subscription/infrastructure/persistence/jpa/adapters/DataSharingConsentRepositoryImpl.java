package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.DataSharingConsentPersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.DataSharingConsentPersistenceEntity;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.DataSharingConsentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link DataSharingConsentRepository} with Spring Data JPA.
 */
@Repository
public class DataSharingConsentRepositoryImpl implements DataSharingConsentRepository {

    private final DataSharingConsentPersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public DataSharingConsentRepositoryImpl(DataSharingConsentPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<DataSharingConsent> findByIdForUpdate(DataSharingConsentId id) {
        return persistenceRepository.findLockedById(UUID.fromString(id.dataSharingConsentId()))
                .map(DataSharingConsentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<DataSharingConsent> findAllByProducerId(ProducerId producerId) {
        return persistenceRepository.findAllByProducerIdOrderByAcceptedAtDesc(UUID.fromString(producerId.producerId()))
                .stream()
                .map(DataSharingConsentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public DataSharingConsent save(DataSharingConsent consent) {
        var id = UUID.fromString(consent.snapshot().id().dataSharingConsentId());
        var entity = persistenceRepository.findById(id).orElseGet(DataSharingConsentPersistenceEntity::new);
        var saved = persistenceRepository.saveAndFlush(
                DataSharingConsentPersistenceAssembler.toPersistenceFromDomain(entity, consent));
        return DataSharingConsentPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
