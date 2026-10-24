package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtensionSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.QuotaExtensionPersistenceEntity;

import java.util.UUID;

/**
 * Assembler between the {@link QuotaExtension} aggregate and its JPA entity.
 */
public final class QuotaExtensionPersistenceAssembler {

    private QuotaExtensionPersistenceAssembler() {
    }

    /**
     * Copies the state of an extension onto an entity, new or managed, leaving the revision to JPA.
     *
     * @param target the entity to update
     * @param domain the extension
     * @return the updated entity
     */
    public static QuotaExtensionPersistenceEntity toPersistenceFromDomain(QuotaExtensionPersistenceEntity target,
                                                                          QuotaExtension domain) {
        var s = domain.snapshot();
        target.setId(UUID.fromString(s.id().quotaExtensionId()));
        target.setSubscriptionId(UUID.fromString(s.subscriptionId().subscriptionId()));
        target.setProducerId(UUID.fromString(s.producerId().producerId()));
        target.setSource(s.source());
        target.setAddedHa(s.addedArea().hectares());
        target.setStatus(s.status());
        target.setBilledMonths(s.billedMonths());
        target.setBaseAmount(s.price() == null ? null : s.price().base().amount());
        target.setTaxAmount(s.price() == null ? null : s.price().tax().amount());
        target.setTotalAmount(s.price() == null ? null : s.price().total().amount());
        target.setExtensionCodeId(s.extensionCodeId() == null ? null : UUID.fromString(s.extensionCodeId().extensionCodeId()));
        target.setPaymentId(s.paymentId());
        target.setRequestedAt(s.createdAt());
        target.setAppliedAt(s.appliedAt());
        return target;
    }

    /**
     * Rebuilds an extension from its JPA entity.
     *
     * @param entity the entity
     * @return the extension
     */
    public static QuotaExtension toDomainFromPersistence(QuotaExtensionPersistenceEntity entity) {
        var price = entity.getTotalAmount() == null ? null : new SubscriptionPrice(new Money(entity.getBaseAmount()),
                new Money(entity.getTaxAmount()), new Money(entity.getTotalAmount()));
        return QuotaExtension.reconstitute(new QuotaExtensionSnapshot(
                new QuotaExtensionId(entity.getId().toString()),
                new SubscriptionId(entity.getSubscriptionId().toString()),
                new ProducerId(entity.getProducerId().toString()),
                entity.getSource(),
                new HectaresQuota(entity.getAddedHa()),
                entity.getStatus(),
                entity.getBilledMonths(),
                price,
                entity.getExtensionCodeId() == null ? null : new ExtensionCodeId(entity.getExtensionCodeId().toString()),
                entity.getPaymentId(),
                entity.getRequestedAt(),
                entity.getAppliedAt(),
                entity.getRevision()));
    }
}
