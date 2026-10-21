package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.InvitationCodePersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler between {@link InvitationCode} and {@link InvitationCodePersistenceEntity}.
 */
public final class InvitationCodePersistenceAssembler {

    private InvitationCodePersistenceAssembler() {
    }

    /**
     * Maps a code into a new JPA entity.
     *
     * @param domain the code
     * @return the new entity
     * @throws IllegalArgumentException if the code is null
     */
    public static InvitationCodePersistenceEntity toPersistenceFromDomain(InvitationCode domain) {
        if (domain == null) {
            throw new IllegalArgumentException("subscription.invitation_code.domain.null");
        }
        var snapshot = domain.snapshot();
        var entity = new InvitationCodePersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().invitationCodeId()));
        entity.setLicenseId(UUID.fromString(snapshot.licenseId().cooperativeLicenseId()));
        entity.setBatchId(snapshot.batchId() == null ? null : UUID.fromString(snapshot.batchId()));
        entity.setCodeHash(snapshot.fingerprint().value());
        entity.setExpiresAt(snapshot.expiresAt());
        return updateEntityFromDomain(entity, domain);
    }

    /**
     * Copies the mutable state of a code onto an entity, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the code
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static InvitationCodePersistenceEntity updateEntityFromDomain(
            InvitationCodePersistenceEntity target, InvitationCode domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("subscription.invitation_code.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setStatus(snapshot.status());
        target.setRedeemedBy(snapshot.redeemedBy() == null ? null : UUID.fromString(snapshot.redeemedBy().producerId()));
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
    public static InvitationCode toDomainFromPersistence(InvitationCodePersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("subscription.invitation_code.entity.null");
        }
        return InvitationCode.reconstitute(new InvitationCodeSnapshot(
                new InvitationCodeId(entity.getId().toString()),
                new CooperativeLicenseId(entity.getLicenseId().toString()),
                entity.getBatchId() == null ? null : entity.getBatchId().toString(),
                new CodeFingerprint(entity.getCodeHash()),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getRedeemedBy() == null ? null : new ProducerId(entity.getRedeemedBy().toString()),
                entity.getRedeemedAt(),
                entity.getRevision()));
    }
}
