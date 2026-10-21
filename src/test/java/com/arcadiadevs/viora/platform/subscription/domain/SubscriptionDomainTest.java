package com.arcadiadevs.viora.platform.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;
class SubscriptionDomainTest {
    private final SubscriptionPricingPolicy pricing = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18"));
    @ParameterizedTest @CsvSource({"0.5,330.00,59.40,389.40", "5,3300.00,594.00,3894.00", "12,7920.00,1425.60,9345.60"})
    void quotesAnnualNetTaxAndGross(String ha, String base, String tax, String total) {
        var price = pricing.quote(new HectaresQuota(new BigDecimal(ha)));
        assertEquals(new BigDecimal(base), price.base().amount());
        assertEquals(new BigDecimal(tax), price.tax().amount());
        assertEquals(new BigDecimal(total), price.total().amount());
    }
    @ParameterizedTest @CsvSource({"0", "-1", "0.1", "0.7", "1.25"})
    void rejectsInvalidCapacity(String ha) {
        assertThrows(IllegalArgumentException.class, () -> new HectaresQuota(new BigDecimal(ha)));
    }
    @Test void calendarYearAdjustsLeapDayAndExcludesExpiry() {
        var period = SubscriptionPeriod.annual(Instant.parse("2028-02-29T15:00:00Z"));
        assertEquals(Instant.parse("2029-02-28T15:00:00Z"), period.endsAt());
        assertTrue(period.contains(period.startsAt()));
        assertFalse(period.contains(period.endsAt()));
    }
    @Test void pendingContractDoesNotGrantAccessAndApprovalIsIdempotent() {
        var now = Instant.parse("2026-10-21T12:00:00Z");
        var quota = new HectaresQuota(new BigDecimal("5"));
        var sub = Subscription.create("owner", "PRODUCER_ANNUAL", quota, pricing.quote(quota), now);
        assertFalse(sub.hasActiveEntitlement(now));
        sub.activate("payment-1", pricing.quote(quota).total(), "PEN", now.plusSeconds(60));
        var expiry = sub.snapshot().period().endsAt();
        sub.activate("payment-1", pricing.quote(quota).total(), "PEN", now.plusSeconds(120));
        assertEquals(expiry, sub.snapshot().period().endsAt());
        assertEquals(1, sub.domainEvents().size());
        assertTrue(sub.hasActiveEntitlement(now.plusSeconds(61)));
        assertThrows(IllegalStateException.class, () -> sub.activate("payment-2", pricing.quote(quota).total(), "PEN", now.plusSeconds(120)));
    }
    @Test void rejectsForgedPaymentTerms() {
        var quota = new HectaresQuota(new BigDecimal("5"));
        var now = Instant.now();
        var sub = Subscription.create("owner", "PRODUCER_ANNUAL", quota, pricing.quote(quota), now);
        assertThrows(IllegalArgumentException.class, () -> sub.activate("p", new Money(BigDecimal.ONE), "PEN", now));
        assertThrows(IllegalArgumentException.class, () -> sub.activate("p", pricing.quote(quota).total(), "USD", now));
    }
}
