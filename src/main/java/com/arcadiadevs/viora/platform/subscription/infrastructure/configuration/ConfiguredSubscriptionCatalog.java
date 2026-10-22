package com.arcadiadevs.viora.platform.subscription.infrastructure.configuration;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;

/** Supplies explicitly configured commercial terms to the individual catalog. */
@Component
public class ConfiguredSubscriptionCatalog implements SubscriptionCatalog {
    private static final String PLAN_CODE = "PRODUCER_ANNUAL";
    private static final String CURRENCY = "PEN";
    private static final BigDecimal MINIMUM_QUOTA = new BigDecimal("0.5");
    private static final BigDecimal QUOTA_INCREMENT = new BigDecimal("0.5");
    private static final int BILLING_MONTHS = 12;
    private final PlanOffer offer;
    private final SubscriptionPricingPolicy pricing;

    /**
     * Builds the offer from application configuration, never from client totals.
     * @param monthlyRate tax-inclusive rate per hectare per month
     * @param taxRate fractional tax rate
     * @param version nonblank tariff identifier
     * @throws IllegalArgumentException if commercial configuration is invalid
     */
    public ConfiguredSubscriptionCatalog(
            @Value("${viora.subscription.monthly-rate}") BigDecimal monthlyRate,
            @Value("${viora.subscription.tax-rate}") BigDecimal taxRate,
            @Value("${viora.subscription.tariff-version}") String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("subscription.tariff.invalid");
        }
        pricing = new SubscriptionPricingPolicy(monthlyRate, taxRate);
        offer = new PlanOffer(PLAN_CODE, CURRENCY, monthlyRate, taxRate,
                MINIMUM_QUOTA, QUOTA_INCREMENT, BILLING_MONTHS, version, true);
    }

    /** @return the available annual offer */
    @Override
    public List<PlanOffer> plans() {
        return List.of(offer);
    }

    /**
     * Calculates a price for the selected configured offer.
     * @param planCode selected offer identifier
     * @param quota validated individual capacity
     * @return immutable annual terms
     * @throws IllegalArgumentException if the plan or capacity is invalid
     */
    @Override
    public SubscriptionPrice quote(String planCode, IndividualHectaresQuota quota) {
        if (!offer.code().equals(planCode)) {
            throw new IllegalArgumentException("subscription.plan.invalid");
        }
        return pricing.quote(quota);
    }
}
