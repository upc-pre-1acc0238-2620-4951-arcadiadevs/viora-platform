package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read representation preserving sponsored identity and optional individual payment terms.
 * @param id contract identity
 * @param mode actual commercial mode
 * @param planCode individual offer, absent for sponsored access
 * @param status effective lifecycle state
 * @param quotaHa covered capacity
 * @param currency individual settlement currency
 * @param baseAmount individual annual base
 * @param taxAmount individual annual tax
 * @param totalAmount individual payable total
 * @param startsAt entitlement start, absent while pending
 * @param endsAt exclusive entitlement end
 * @param entitlementActive current access
 * @param cooperativeId sponsoring cooperative, when applicable
 */
public record SubscriptionDetailsResource(String id, String mode, String planCode, String status,
        BigDecimal quotaHa, String currency, BigDecimal baseAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, Instant startsAt, Instant endsAt, boolean entitlementActive, String cooperativeId) { }
