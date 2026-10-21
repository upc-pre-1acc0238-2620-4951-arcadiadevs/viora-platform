package com.arcadiadevs.viora.platform.subscription.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import static org.junit.jupiter.api.Assertions.*;

class IndividualPricingTest {
    private final SubscriptionPricingPolicy pricing = new SubscriptionPricingPolicy(
            new BigDecimal("55"), new BigDecimal("0.18"));

    @ParameterizedTest
    @CsvSource({"0.5,330.00,59.40,389.40", "5,3300.00,594.00,3894.00", "12,7920.00,1425.60,9345.60"})
    void calculatesAnnualBaseTaxAndTotal(String hectares, String base, String tax, String total) {
        var price = pricing.quote(new IndividualHectaresQuota(new BigDecimal(hectares)));
        assertEquals(new BigDecimal(base), price.base().amount());
        assertEquals(new BigDecimal(tax), price.tax().amount());
        assertEquals(new BigDecimal(total), price.total().amount());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "0.1", "0.7", "1.25", "1000000000000000000"})
    void rejectsInvalidPurchaseCapacity(String value) {
        assertThrows(IllegalArgumentException.class, () -> new IndividualHectaresQuota(new BigDecimal(value)));
    }

    @Test
    void rejectsMonetaryOverflowBeforePersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> pricing.quote(new IndividualHectaresQuota(new BigDecimal("100000000000000000"))));
    }

    @Test
    void rejectsFractionalCentsWithDomainErrorKey() {
        var error = assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("0.001")));
        assertEquals("subscription.money.invalid", error.getMessage());
    }

    @Test
    void usesSuppliedRatesAndHalfUpRounding() {
        var custom = new SubscriptionPricingPolicy(new BigDecimal("0.055"), new BigDecimal("0.5"));
        var price = custom.quote(new IndividualHectaresQuota(new BigDecimal("0.5")));
        assertEquals(new BigDecimal("0.33"), price.base().amount());
        assertEquals(new BigDecimal("0.17"), price.tax().amount());
        assertEquals(new BigDecimal("0.50"), price.total().amount());
    }
}
