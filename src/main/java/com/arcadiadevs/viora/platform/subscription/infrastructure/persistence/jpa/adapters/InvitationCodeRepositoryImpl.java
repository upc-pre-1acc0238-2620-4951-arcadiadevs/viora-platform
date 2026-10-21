package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.InvitationCodePersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link InvitationCodeRepository} with Spring Data JPA.
 */
@Repository
public class InvitationCodeRepositoryImpl implements InvitationCodeRepository {

    private final InvitationCodePersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public InvitationCodeRepositoryImpl(InvitationCodePersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<InvitationCode> findByFingerprintForRedemption(CodeFingerprint fingerprint) {
        return persistenceRepository.findByCodeHash(fingerprint.value())
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public InvitationCode save(InvitationCode code) {
        var id = UUID.fromString(code.snapshot().id().invitationCodeId());
        var entity = persistenceRepository.findById(id)
                .map(existing -> InvitationCodePersistenceAssembler.updateEntityFromDomain(existing, code))
                .orElseGet(() -> InvitationCodePersistenceAssembler.toPersistenceFromDomain(code));
        return InvitationCodePersistenceAssembler.toDomainFromPersistence(persistenceRepository.saveAndFlush(entity));
    }
}
