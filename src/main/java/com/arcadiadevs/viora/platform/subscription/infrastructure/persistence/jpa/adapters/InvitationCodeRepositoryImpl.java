package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.InvitationCodePersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.InvitationCodePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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
    public Optional<InvitationCode> findByFingerprint(CodeFingerprint fingerprint) {
        return persistenceRepository.findUnlockedByCodeHash(fingerprint.value())
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<InvitationCode> findByIdForUpdate(InvitationCodeId id) {
        return persistenceRepository.findLockedById(UUID.fromString(id.invitationCodeId()))
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<InvitationCode> findAllByLicenseId(CooperativeLicenseId licenseId) {
        return persistenceRepository.findAllOfLicense(UUID.fromString(licenseId.cooperativeLicenseId())).stream()
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<InvitationCode> findOverdueByLicenseId(CooperativeLicenseId licenseId, Instant at) {
        return persistenceRepository.findOverdue(UUID.fromString(licenseId.cooperativeLicenseId()), at).stream()
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public int countSeatsInUse(CooperativeLicenseId licenseId, Instant at) {
        return Math.toIntExact(persistenceRepository.countSeatsInUse(
                UUID.fromString(licenseId.cooperativeLicenseId()), at));
    }

    @Override
    public boolean existsByFingerprint(CodeFingerprint fingerprint) {
        return persistenceRepository.existsByCodeHash(fingerprint.value());
    }

    @Override
    public InvitationCode save(InvitationCode code) {
        var id = UUID.fromString(code.snapshot().id().invitationCodeId());
        var entity = persistenceRepository.findById(id)
                .map(existing -> InvitationCodePersistenceAssembler.updateEntityFromDomain(existing, code))
                .orElseGet(() -> InvitationCodePersistenceAssembler.toPersistenceFromDomain(code));
        return InvitationCodePersistenceAssembler.toDomainFromPersistence(persistenceRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<InvitationCode> findById(InvitationCodeId id) {
        return persistenceRepository.findById(UUID.fromString(id.invitationCodeId()))
                .map(InvitationCodePersistenceAssembler::toDomainFromPersistence);
    }
}
