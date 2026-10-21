package com.arcadiadevs.viora.platform.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;

class IndividualSubscriptionDomainTest {
    private final Instant createdAt = Instant.parse("2028-02-28T12:00:00Z");
    private final SubscriptionPrice price = new SubscriptionPricingPolicy(new BigDecimal("55"),
            new BigDecimal("0.18")).quote(new IndividualHectaresQuota(new BigDecimal("0.5")));

    private Subscription pending() {
        return Subscription.createIndividual(new ProducerId("550e8400-e29b-41d4-a716-446655440000"),
                "PRODUCER_ANNUAL", new IndividualHectaresQuota(new BigDecimal("0.5")), price, createdAt);
    }

    @Test
    void createsPendingWithoutDatesOrAccess() {
        var subscription = pending();
        assertEquals(PlanMode.PLAN_PRODUCTOR_INDIVIDUAL, subscription.snapshot().mode());
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, subscription.snapshot().status());
        assertNull(subscription.snapshot().period());
        assertFalse(subscription.hasActiveEntitlement(createdAt));
        assertEquals(subscription.snapshot(), Subscription.reconstitute(subscription.snapshot()).snapshot());
    }

    @Test
    void activatesForCalendarYearAndReplaysWithoutExtension() {
        var subscription = pending();
        var approvedAt = Instant.parse("2028-02-29T12:00:00Z");
        subscription.activateIndividual("12345", price.total(), "PEN", approvedAt);
        var state = subscription.snapshot();
        subscription.activateIndividual("12345", price.total(), "PEN", approvedAt.plusSeconds(5));
        assertEquals(state, subscription.snapshot());
        assertEquals(Instant.parse("2029-02-28T12:00:00Z"), state.period().endsAt());
        assertEquals(1, subscription.domainEvents().size());
        assertTrue(subscription.hasActiveEntitlement(approvedAt));
        assertFalse(subscription.hasActiveEntitlement(state.period().endsAt()));
        assertEquals(state, Subscription.reconstitute(state).snapshot());
    }

    @Test
    void rejectsMismatchedOrPrematurePayment() {
        var subscription = pending();
        assertThrows(IllegalArgumentException.class,
                () -> subscription.activateIndividual("12345", new Money(BigDecimal.ONE), "PEN", createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> subscription.activateIndividual("12345", price.total(), "USD", createdAt));
        assertThrows(IllegalStateException.class,
                () -> subscription.activateIndividual("12345", price.total(), "PEN", createdAt.minusSeconds(1)));
        assertFalse(subscription.hasActiveEntitlement(createdAt));
    }

    @Test
    void rejectsActiveSnapshotWithoutPaymentEvidence() {
        var s = pending().snapshot();
        var invalid = new SubscriptionSnapshot(s.id(), s.producerId(), s.mode(), SubscriptionStatus.ACTIVE,
                s.quota(), SubscriptionPeriod.annual(createdAt), null, null, null, s.revision(),
                s.planCode(), s.price(), null, s.createdAt());
        assertThrows(IllegalArgumentException.class, () -> Subscription.reconstitute(invalid));
    }

    @Test
    void rejectsMissingOwnerAndMixedModeReferences() {
        var s = pending().snapshot();
        var invalid = new SubscriptionSnapshot(s.id(), null, s.mode(), s.status(), s.quota(), null,
                null, null, null, s.revision(), s.planCode(), s.price(), null, s.createdAt());
        assertThrows(IllegalArgumentException.class, () -> Subscription.reconstitute(invalid));
        assertThrows(IllegalArgumentException.class, () -> Subscription.createIndividual(s.producerId(),
                null, new IndividualHectaresQuota(new BigDecimal("0.5")), s.price(), s.createdAt()));
    }
}
