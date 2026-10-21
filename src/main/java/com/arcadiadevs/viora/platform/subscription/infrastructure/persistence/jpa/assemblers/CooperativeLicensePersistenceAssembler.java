package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.CooperativeLicensePersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler between {@link CooperativeLicense} and {@link CooperativeLicensePersistenceEntity}.
 */
public final class CooperativeLicensePersistenceAssembler {

    private CooperativeLicensePersistenceAssembler() {
    }

    /**
     * Maps a license into a new JPA entity.
     *
     * @param domain the license
     * @return the new entity
     * @throws IllegalArgumentException if the license is null
     */
    public static CooperativeLicensePersistenceEntity toPersistenceFromDomain(CooperativeLicense domain) {
        if (domain == null) {
            throw new IllegalArgumentException("subscription.license.domain.null");
        }
        var entity = new CooperativeLicensePersistenceEntity();
        entity.setId(UUID.fromString(domain.snapshot().id().cooperativeLicenseId()));
        entity.setCooperativeId(UUID.fromString(domain.snapshot().cooperativeId().cooperativeId()));
        return updateEntityFromDomain(entity, domain);
    }

    /**
     * Copies the mutable state of a license onto an entity, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the license
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static CooperativeLicensePersistenceEntity updateEntityFromDomain(
            CooperativeLicensePersistenceEntity target, CooperativeLicense domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("subscription.license.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setCooperativeName(snapshot.cooperativeName());
        target.setSeats(snapshot.seats());
        target.setContractedAreaHa(snapshot.contractedArea().hectares());
        target.setCommittedAreaHa(snapshot.committedAreaHa());
        target.setStartsAt(snapshot.period().startsAt());
        target.setEndsAt(snapshot.period().endsAt());
        return target;
    }

    /**
     * Rebuilds a license from its JPA entity.
     *
     * @param entity the entity
     * @return the license
     * @throws IllegalArgumentException if the entity is null
     */
    public static CooperativeLicense toDomainFromPersistence(CooperativeLicensePersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("subscription.license.entity.null");
        }
        return CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(
                new CooperativeLicenseId(entity.getId().toString()),
                new CooperativeId(entity.getCooperativeId().toString()),
                entity.getCooperativeName(),
                entity.getSeats(),
                new HectaresQuota(entity.getContractedAreaHa()),
                entity.getCommittedAreaHa(),
                new SubscriptionPeriod(entity.getStartsAt(), entity.getEndsAt()),
                entity.getRevision()));
    }
}
