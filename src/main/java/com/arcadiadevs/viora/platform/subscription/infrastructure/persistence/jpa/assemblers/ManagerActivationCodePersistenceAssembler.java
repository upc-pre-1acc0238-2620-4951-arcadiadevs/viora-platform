package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.ManagerActivationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.ManagerActivationCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.ManagerActivationCodePersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler between {@link ManagerActivationCode} and its JPA entity.
 */
public final class ManagerActivationCodePersistenceAssembler {

    private ManagerActivationCodePersistenceAssembler() {
    }

    /**
     * Copies the state of a code onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the code
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static ManagerActivationCodePersistenceEntity toPersistenceFromDomain(
            ManagerActivationCodePersistenceEntity target, ManagerActivationCode domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("subscription.manager_code.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setId(UUID.fromString(snapshot.id().managerCodeId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().cooperativeLicenseId()));
        target.setCodeHash(snapshot.fingerprint().value());
        target.setStatus(snapshot.status());
        target.setExpiresAt(snapshot.expiresAt());
        target.setRedeemedBy(snapshot.redeemedBy() == null ? null : UUID.fromString(snapshot.redeemedBy().managerId()));
        target.setRedeemedAt(snapshot.redeemedAt());
        return target;
    }

    /**
     * Rebuilds a code from its JPA entity.
     *
     * @param entity the entity
     * @return the code
     * @throws IllegalArgumentException if the entity is null
     */
    public static ManagerActivationCode toDomainFromPersistence(ManagerActivationCodePersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("subscription.manager_code.entity.null");
        }
        return ManagerActivationCode.reconstitute(new ManagerActivationCodeSnapshot(
                new ManagerCodeId(entity.getId().toString()),
                new CooperativeLicenseId(entity.getLicenseId().toString()),
                new CodeFingerprint(entity.getCodeHash()),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getRedeemedBy() == null ? null : new ManagerId(entity.getRedeemedBy().toString()),
                entity.getRedeemedAt(),
                entity.getRevision()));
    }
}
