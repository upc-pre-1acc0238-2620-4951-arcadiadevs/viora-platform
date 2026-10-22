package com.arcadiadevs.viora.platform.subscription.infrastructure;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.subscription.infrastructure.configuration.ConfiguredSubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguredSubscriptionCatalogTest {
    @Test
    void customConfigurationChangesCatalogAndQuoteTogether() {
        var catalog = new ConfiguredSubscriptionCatalog(new BigDecimal("10"), new BigDecimal("0.1"), "fixture-2");
        var offer = catalog.plans().getFirst();
        assertEquals("fixture-2", offer.tariffVersion());
        assertTrue(offer.taxIncluded());
        assertEquals(new BigDecimal("10"), offer.monthlyRatePerHa());
        assertEquals(new BigDecimal("60.00"), catalog.quote(offer.code(),
                new IndividualHectaresQuota(new BigDecimal("0.5"))).total().amount());
    }

    @Test
    void rejectsInvalidConfigurationAndUnknownPlan() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguredSubscriptionCatalog(BigDecimal.ZERO, BigDecimal.ONE, "v1"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguredSubscriptionCatalog(BigDecimal.ONE, BigDecimal.ONE, " "));
        var catalog = new ConfiguredSubscriptionCatalog(BigDecimal.ONE, BigDecimal.ONE, "v1");
        assertThrows(IllegalArgumentException.class,
                () -> catalog.quote("UNKNOWN", new IndividualHectaresQuota(new BigDecimal("0.5"))));
    }
}
