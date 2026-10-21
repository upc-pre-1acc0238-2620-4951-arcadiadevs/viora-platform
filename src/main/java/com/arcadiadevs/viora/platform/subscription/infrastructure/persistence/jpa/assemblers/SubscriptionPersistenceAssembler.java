package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.SubscriptionSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;

import java.util.UUID;

/**
 * Unified persistence assembler between {@link Subscription} and {@link SubscriptionPersistenceEntity}.
 */
public final class SubscriptionPersistenceAssembler {

    private SubscriptionPersistenceAssembler() {
    }

    /**
     * Maps a subscription into a new JPA entity.
     *
     * @param domain the subscription
     * @return the new entity
     * @throws IllegalArgumentException if the subscription is null
     */
    public static SubscriptionPersistenceEntity toPersistenceFromDomain(Subscription domain) {
        if (domain == null) {
            throw new IllegalArgumentException("subscription.domain.null");
        }
        var snapshot = domain.snapshot();
        var entity = new SubscriptionPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().subscriptionId()));
        entity.setProducerId(UUID.fromString(snapshot.producerId().producerId()));
        entity.setMode(snapshot.mode());
        entity.setCooperativeId(snapshot.cooperativeId() == null ? null
                : UUID.fromString(snapshot.cooperativeId().cooperativeId()));
        entity.setLicenseId(snapshot.licenseId() == null ? null
                : UUID.fromString(snapshot.licenseId().cooperativeLicenseId()));
        entity.setRedeemedCodeId(snapshot.redeemedCodeId() == null ? null
                : UUID.fromString(snapshot.redeemedCodeId().invitationCodeId()));
        return updateEntityFromDomain(entity, domain);
    }

    /**
     * Copies the mutable state of a subscription onto an entity, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the subscription
     * @return the updated entity
     * @throws IllegalArgumentException if an argument is null
     */
    public static SubscriptionPersistenceEntity updateEntityFromDomain(
            SubscriptionPersistenceEntity target, Subscription domain) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("subscription.entity_or_domain.null");
        }
        var snapshot = domain.snapshot();
        target.setStatus(snapshot.status());
        target.setQuotaHa(snapshot.quota().hectares());
        target.setStartsAt(snapshot.period().startsAt());
        target.setEndsAt(snapshot.period().endsAt());
        return target;
    }

    /**
     * Rebuilds a subscription from its JPA entity.
     *
     * @param entity the entity
     * @return the subscription
     * @throws IllegalArgumentException if the entity is null
     */
    public static Subscription toDomainFromPersistence(SubscriptionPersistenceEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("subscription.entity.null");
        }
        return Subscription.reconstitute(new SubscriptionSnapshot(
                new SubscriptionId(entity.getId().toString()),
                new ProducerId(entity.getProducerId().toString()),
                entity.getMode(),
                entity.getStatus(),
                new HectaresQuota(entity.getQuotaHa()),
                new SubscriptionPeriod(entity.getStartsAt(), entity.getEndsAt()),
                entity.getCooperativeId() == null ? null : new CooperativeId(entity.getCooperativeId().toString()),
                entity.getLicenseId() == null ? null : new CooperativeLicenseId(entity.getLicenseId().toString()),
                entity.getRedeemedCodeId() == null ? null
                        : new InvitationCodeId(entity.getRedeemedCodeId().toString()),
                entity.getRevision()));
    }
}
