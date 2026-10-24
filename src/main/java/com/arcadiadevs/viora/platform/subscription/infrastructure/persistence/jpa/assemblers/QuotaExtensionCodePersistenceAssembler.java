package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtensionCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtensionCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionCodePersistenceEntity;

import java.util.UUID;

/**
 * Assembler between the {@link QuotaExtensionCode} aggregate and its JPA entity.
 */
public final class QuotaExtensionCodePersistenceAssembler {

    private QuotaExtensionCodePersistenceAssembler() {
    }

    /**
     * Copies the state of a code onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the code
     * @return the updated entity
     */
    public static QuotaExtensionCodePersistenceEntity toPersistenceFromDomain(QuotaExtensionCodePersistenceEntity target,
                                                                              QuotaExtensionCode domain) {
        var s = domain.snapshot();
        target.setId(UUID.fromString(s.id().extensionCodeId()));
        target.setLicenseId(UUID.fromString(s.licenseId().cooperativeLicenseId()));
        target.setCodeHash(s.fingerprint().value());
        target.setAddedHa(s.addedArea().hectares());
        target.setStatus(s.status());
        target.setExpiresAt(s.expiresAt());
        return target;
    }

    /**
     * Rebuilds a code from its JPA entity.
     *
     * @param entity the entity
     * @return the code
     */
    public static QuotaExtensionCode toDomainFromPersistence(QuotaExtensionCodePersistenceEntity entity) {
        return QuotaExtensionCode.reconstitute(new QuotaExtensionCodeSnapshot(
                new ExtensionCodeId(entity.getId().toString()),
                new CooperativeLicenseId(entity.getLicenseId().toString()),
                new CodeFingerprint(entity.getCodeHash()),
                new HectaresQuota(entity.getAddedHa()),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getRevision()));
    }
}
