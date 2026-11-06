package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsentSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ConsentTextVersion;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.DataSharingConsentPersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler between {@link DataSharingConsent} and its JPA entity.
 */
public final class DataSharingConsentPersistenceAssembler {

    private DataSharingConsentPersistenceAssembler() {
    }

    /**
     * Copies the state of a consent onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the consent
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static DataSharingConsentPersistenceEntity toPersistenceFromDomain(
            DataSharingConsentPersistenceEntity target, DataSharingConsent domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("subscription.consent.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setId(UUID.fromString(snapshot.id().dataSharingConsentId()));
        target.setProducerId(UUID.fromString(snapshot.producerId().producerId()));
        target.setLicenseId(UUID.fromString(snapshot.licenseId().cooperativeLicenseId()));
        target.setSubscriptionId(UUID.fromString(snapshot.subscriptionId().subscriptionId()));
        target.setManagerId(snapshot.managerId() == null ? null : UUID.fromString(snapshot.managerId().managerId()));
        target.setTextVersion(snapshot.textVersion().value());
        target.setAcceptedAt(snapshot.acceptedAt());
        target.setEndsAt(snapshot.endsAt());
        target.setWithdrawnAt(snapshot.withdrawnAt());
        return target;
    }

    /**
     * Rebuilds a consent from its JPA entity.
     *
     * @param entity the entity
     * @return the consent
     * @throws IllegalArgumentException if the entity is null
     */
    public static DataSharingConsent toDomainFromPersistence(DataSharingConsentPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("subscription.consent.entity.null");
        }
        return DataSharingConsent.reconstitute(new DataSharingConsentSnapshot(
                new DataSharingConsentId(entity.getId().toString()),
                new ProducerId(entity.getProducerId().toString()),
                new CooperativeLicenseId(entity.getLicenseId().toString()),
                new SubscriptionId(entity.getSubscriptionId().toString()),
                entity.getManagerId() == null ? null : new ManagerId(entity.getManagerId().toString()),
                new ConsentTextVersion(entity.getTextVersion()),
                entity.getAcceptedAt(),
                entity.getEndsAt(),
                entity.getWithdrawnAt(),
                entity.getRevision()));
    }
}
