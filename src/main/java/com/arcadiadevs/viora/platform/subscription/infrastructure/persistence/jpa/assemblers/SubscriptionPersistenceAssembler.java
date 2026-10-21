package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
/** Explicit bidirectional persistence mapping. */
public final class SubscriptionPersistenceAssembler {
    private SubscriptionPersistenceAssembler() { }
    public static SubscriptionPersistenceEntity toPersistenceFromDomain(Subscription domain, SubscriptionPersistenceEntity row) {
        var s = domain.snapshot();
        row.setId(s.id()); row.setProducerId(s.producerId()); row.setPlanCode(s.planCode());
        row.setQuotaHa(s.quota().value()); row.setBaseAmount(s.price().base().amount());
        row.setTaxAmount(s.price().tax().amount()); row.setTotalAmount(s.price().total().amount());
        row.setStatus(s.status()); row.setPaymentId(s.paymentId()); row.setRequestedAt(s.createdAt());
        row.setStartsAt(s.period() == null ? null : s.period().startsAt());
        row.setEndsAt(s.period() == null ? null : s.period().endsAt());
        return row;
    }
    public static Subscription toDomainFromPersistence(SubscriptionPersistenceEntity row) {
        var period = row.getStartsAt() == null ? null : new SubscriptionPeriod(row.getStartsAt(), row.getEndsAt());
        return Subscription.reconstitute(new SubscriptionSnapshot(row.getId(), row.getProducerId(), row.getPlanCode(),
                new HectaresQuota(row.getQuotaHa()), new SubscriptionPrice(new Money(row.getBaseAmount()),
                new Money(row.getTaxAmount()), new Money(row.getTotalAmount())), row.getStatus(), period,
                row.getPaymentId(), row.getRequestedAt()));
    }
}
