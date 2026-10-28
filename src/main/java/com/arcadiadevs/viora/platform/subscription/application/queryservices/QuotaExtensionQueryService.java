package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Read boundary of plan extensions, always scoped to the authenticated producer.
 */
public interface QuotaExtensionQueryService {

    /**
     * Tells how an owned plan can grow now, so the app can price an extension before requesting it.
     *
     * @param owner          the authenticated producer
     * @param subscriptionId the plan
     * @return the terms, or empty if the plan does not exist or belongs to someone else
     */
    Optional<ExtensionTerms> terms(String owner, UUID subscriptionId);

    /**
     * Finds an extension of an owned plan.
     *
     * @param owner          the authenticated producer
     * @param subscriptionId the plan
     * @param extensionId    the extension
     * @return the extension, or empty if it does not exist or belongs to another plan or producer
     */
    Optional<QuotaExtension> find(String owner, UUID subscriptionId, UUID extensionId);

    /**
     * Reads the latest stored payment state of an extension found with {@link #find}.
     *
     * @param extensionId the extension
     * @return the attempt state, or null when no payment was attempted
     */
    String paymentStatus(UUID extensionId);

    /**
     * How a plan can grow now.
     *
     * @param subscriptionId   the plan
     * @param mode             individual or sponsored
     * @param extendable       whether hectares can be added now
     * @param reason           why not, such as {@code subscription.extension.renewal_window}; null when extendable
     * @param action           {@code PAY} for an individual plan, {@code CODE} for a sponsored one, {@code RENEW} in
     *                         the renewal window of an individual plan, or {@code NONE}
     * @param quotaHa          the hectares the plan covers now
     * @param endsAt           when the plan ends, which an extension does not change
     * @param billableMonths   the complete months a paid extension bills now; null unless {@code PAY}
     * @param monthlyRatePerHa the tax-included rate per hectare and month; null unless {@code PAY}
     * @param currency         the settlement currency; null unless {@code PAY}
     * @param minimumHa        the smallest paid extension; null unless {@code PAY}
     * @param incrementHa      the step of a paid extension; null unless {@code PAY}
     * @param billableDays     the days a paid extension bills now; null unless {@code PAY}
     * @param extensionRatePerHa the tax-included price of one hectare for those days, which times the hectares
     *                         and rounded to cents is the total charged; null unless {@code PAY}
     */
    record ExtensionTerms(UUID subscriptionId, String mode, boolean extendable, String reason, String action,
                          BigDecimal quotaHa, Instant endsAt, Integer billableMonths, BigDecimal monthlyRatePerHa,
                          String currency, BigDecimal minimumHa, BigDecimal incrementHa, Integer billableDays,
                          BigDecimal extensionRatePerHa) { }
}
