package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public representation of hectares added to a plan.
 *
 * @param id             the extension identifier
 * @param subscriptionId the plan that grows
 * @param source         {@code PAYMENT} or {@code CODE}
 * @param status         {@code PENDING_PAYMENT} or {@code APPLIED}
 * @param addedHa        the hectares added
 * @param quotaHa        the hectares the plan covers now, including this extension once applied
 * @param endsAt         when the plan ends, which the extension does not change
 * @param billedMonths   the months a payment covers; null for a code
 * @param currency       the settlement currency; null for a code
 * @param baseAmount     the net amount; null for a code
 * @param taxAmount      the tax amount; null for a code
 * @param totalAmount    the payable amount; null for a code
 * @param requestedAt    when it was requested
 * @param appliedAt      when the hectares were added; null while pending
 * @param paymentStatus  the latest payment attempt state; null for a code or before a checkout
 */
@Schema(name = "QuotaExtensionResource", description = "Hectares added to a plan without changing when it ends")
public record QuotaExtensionResource(
        String id,
        String subscriptionId,
        @Schema(example = "PAYMENT") String source,
        @Schema(example = "PENDING_PAYMENT") String status,
        @Schema(example = "3.00") BigDecimal addedHa,
        @Schema(example = "15.00") BigDecimal quotaHa,
        Instant endsAt,
        Integer billedMonths,
        String currency,
        BigDecimal baseAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Instant requestedAt,
        Instant appliedAt,
        String paymentStatus
) {
}
