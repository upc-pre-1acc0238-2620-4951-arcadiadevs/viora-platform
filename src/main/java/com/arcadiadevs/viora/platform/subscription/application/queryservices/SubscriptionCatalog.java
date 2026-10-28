package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.math.BigDecimal;
import java.util.List;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;

/** Server-owned read contract for individual subscription offers and quotes. */
public interface SubscriptionCatalog {
    /** @return available individual offers */
    List<PlanOffer> plans();

    /**
     * Quotes a known plan without accepting client-supplied monetary amounts.
     * @param planCode selected catalog code
     * @param quota validated purchase capacity
     * @return server-calculated payment terms
     * @throws IllegalArgumentException if the plan or capacity is invalid
     */
    SubscriptionPrice quote(String planCode, IndividualHectaresQuota quota);

    /**
     * Quotes hectares added to a plan in force for the days it has left.
     * @param planCode the plan's catalog code
     * @param addedQuota validated hectares to add
     * @param days days left of the plan
     * @param periodDays days of the whole plan
     * @return server-calculated payment terms
     * @throws IllegalArgumentException if the plan, hectares or days are invalid
     */
    SubscriptionPrice quoteExtension(String planCode, IndividualHectaresQuota addedQuota, int days, int periodDays);

    /**
     * The tax-included price of one hectare for the days left of a plan, which the app multiplies by the hectares.
     * @param planCode the plan's catalog code
     * @param days days left of the plan
     * @param periodDays days of the whole plan
     * @return PEN per hectare
     * @throws IllegalArgumentException if the plan or days are invalid
     */
    BigDecimal extensionRatePerHa(String planCode, int days, int periodDays);

    /**
     * Public commercial conditions supplied by configuration and domain policy.
     * @param code plan identifier
     * @param currency settlement currency
     * @param monthlyRatePerHa monthly hectare rate interpreted according to taxIncluded
     * @param taxRate fractional tax rate
     * @param minimumQuotaHa smallest selectable capacity
     * @param quotaIncrementHa selectable increment
     * @param billingMonths months billed per purchase
     * @param tariffVersion version of the quoted tariff
     * @param taxIncluded whether the monthly rate already contains tax
     */
    record PlanOffer(String code, String currency, BigDecimal monthlyRatePerHa, BigDecimal taxRate,
            BigDecimal minimumQuotaHa, BigDecimal quotaIncrementHa, int billingMonths, String tariffVersion, boolean taxIncluded) { }
}
