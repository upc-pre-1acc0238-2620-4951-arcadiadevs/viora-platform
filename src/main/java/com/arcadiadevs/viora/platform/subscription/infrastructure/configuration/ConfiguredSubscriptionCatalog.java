package com.arcadiadevs.viora.platform.subscription.infrastructure.configuration;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
/** Configured tariff adapter; prices never come from editable client totals. */
@Component
public class ConfiguredSubscriptionCatalog implements SubscriptionCatalog {
    private final PlanOffer offer;
    private final SubscriptionPricingPolicy pricing;
    /** @param monthlyRate net monthly rate @param taxRate fractional tax @param version immutable tariff identifier */
    public ConfiguredSubscriptionCatalog(@Value("${viora.subscription.monthly-rate:55}") BigDecimal monthlyRate,
            @Value("${viora.subscription.tax-rate:0.18}") BigDecimal taxRate,
            @Value("${viora.subscription.tariff-version:producer-annual-2026-1}") String version) {
        pricing = new SubscriptionPricingPolicy(monthlyRate, taxRate);
        offer = new PlanOffer("PRODUCER_ANNUAL", "PEN", monthlyRate, taxRate,
                new BigDecimal("0.5"), new BigDecimal("0.5"), 12, version);
    }
    @Override public List<PlanOffer> plans() { return List.of(offer); }
    @Override public SubscriptionPrice quote(String planCode, HectaresQuota quota) {
        if (!offer.code().equals(planCode)) { throw new IllegalArgumentException("subscription.plan.invalid"); }
        return pricing.quote(quota);
    }
}
