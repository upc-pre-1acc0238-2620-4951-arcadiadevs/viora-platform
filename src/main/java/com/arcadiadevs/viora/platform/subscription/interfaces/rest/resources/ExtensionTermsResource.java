package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * How a plan can grow now, so the app can price a paid extension as the producer picks the hectares.
 *
 * @param subscriptionId   the plan
 * @param mode             {@code PLAN_PRODUCTOR_INDIVIDUAL} or {@code PLAN_COOPERATIVO_PATROCINADO}
 * @param extendable       whether hectares can be added now
 * @param reason           why not; null when extendable
 * @param action           {@code PAY}, {@code CODE}, {@code RENEW} or {@code NONE}
 * @param quotaHa          the hectares the plan covers now
 * @param endsAt           when the plan ends
 * @param billableMonths   the complete months a paid extension bills now; null unless {@code PAY}
 * @param monthlyRatePerHa the tax-included rate per hectare and month; null unless {@code PAY}
 * @param currency         the settlement currency; null unless {@code PAY}
 * @param minimumHa        the smallest paid extension; null unless {@code PAY}
 * @param incrementHa      the step of a paid extension; null unless {@code PAY}
 * @param billableDays     the days a paid extension bills now; null unless {@code PAY}
 * @param extensionRatePerHa the tax-included price of one hectare for those days: the total charged is the hectares
 *                         times this rate, rounded to cents; null unless {@code PAY}
 */
@Schema(name = "ExtensionTermsResource", description = "How a plan in force can grow now")
public record ExtensionTermsResource(
        String subscriptionId,
        String mode,
        boolean extendable,
        String reason,
        @Schema(example = "PAY") String action,
        @Schema(example = "12.00") BigDecimal quotaHa,
        Instant endsAt,
        @Schema(example = "10", description = "Complete months left; informative, the price follows the days")
        Integer billableMonths,
        @Schema(example = "55") BigDecimal monthlyRatePerHa,
        @Schema(example = "PEN") String currency,
        @Schema(example = "0.5") BigDecimal minimumHa,
        @Schema(example = "0.5") BigDecimal incrementHa,
        @Schema(example = "300") Integer billableDays,
        @Schema(example = "542.465753", description = "PEN per hectare for the days left; total = hectares x rate, "
                + "rounded to cents") BigDecimal extensionRatePerHa
) {
}
