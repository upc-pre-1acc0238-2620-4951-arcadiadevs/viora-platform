package com.arcadiadevs.viora.platform.subscription.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.SubscriptionSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.SubscriptionPersistenceAssembler;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IndividualSubscriptionPersistenceTest {
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private SubscriptionPersistenceRepository rows;

    @Test
    void pendingAndApprovedIndividualTermsSurvivePersistenceWithoutSponsor() {
        var quota = new IndividualHectaresQuota(new BigDecimal("0.5"));
        var price = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18")).quote(quota);
        var createdAt = Instant.parse("2028-02-28T12:00:00Z");
        var original = Subscription.createIndividual(new ProducerId(UUID.randomUUID().toString()),
                "PRODUCER_ANNUAL", quota, price, createdAt);
        var pending = subscriptions.save(original);
        assertEquals(original.snapshot().id(), pending.snapshot().id());
        assertEquals(price, pending.snapshot().price());
        assertNull(pending.snapshot().period());
        assertNull(pending.snapshot().cooperativeId());
        assertFalse(pending.hasActiveEntitlement(createdAt));

        var approval = Instant.parse("2028-02-29T12:00:00Z");
        pending.activateIndividual("test-" + UUID.randomUUID(), price.total(), "PEN", approval);
        var active = subscriptions.save(pending);
        var id = UUID.fromString(active.snapshot().id().subscriptionId());
        var reloaded = SubscriptionPersistenceAssembler.toDomainFromPersistence(rows.findById(id).orElseThrow());
        assertEquals(SubscriptionStatus.ACTIVE, reloaded.snapshot().status());
        assertEquals(price, reloaded.snapshot().price());
        assertEquals(active.snapshot().paymentId(), reloaded.snapshot().paymentId());
        assertEquals(Instant.parse("2029-02-28T12:00:00Z"), reloaded.snapshot().period().endsAt());
        assertTrue(reloaded.hasActiveEntitlement(approval));
        assertEquals(1, reloaded.snapshot().revision());
    }
    @Test
    void sponsoredPersistenceKeepsItsQuotaLicensePeriodAndReferences() {
        var owner = new ProducerId(UUID.randomUUID().toString());
        var start = Instant.parse("2028-02-29T12:00:00Z");
        var licenseEnd = start.plusSeconds(20 * 24 * 60 * 60);
        var snapshot = new SubscriptionSnapshot(new SubscriptionId(), owner,
                PlanMode.PLAN_COOPERATIVO_PATROCINADO, SubscriptionStatus.ACTIVE,
                new HectaresQuota(new BigDecimal("0.25")), new SubscriptionPeriod(start, licenseEnd),
                new CooperativeId(UUID.randomUUID().toString()),
                new CooperativeLicenseId(UUID.randomUUID().toString()),
                new InvitationCodeId(UUID.randomUUID().toString()), 0L);
        var stored = subscriptions.save(Subscription.reconstitute(snapshot));
        assertEquals(snapshot.mode(), stored.snapshot().mode());
        assertEquals(snapshot.quota(), stored.snapshot().quota());
        assertEquals(licenseEnd, stored.snapshot().period().endsAt());
        assertEquals(snapshot.cooperativeId(), stored.snapshot().cooperativeId());
        assertEquals(snapshot.licenseId(), stored.snapshot().licenseId());
        assertEquals(snapshot.redeemedCodeId(), stored.snapshot().redeemedCodeId());
        assertNull(stored.snapshot().price());
        assertNull(stored.snapshot().paymentId());
        assertTrue(subscriptions.existsActiveByProducerId(owner, start));
        assertFalse(subscriptions.existsActiveByProducerId(owner, licenseEnd));
    }

}
