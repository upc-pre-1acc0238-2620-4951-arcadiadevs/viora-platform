package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
/** Public contract snapshot, including server-calculated tax and entitlement. */
public record SubscriptionResource(UUID id, String mode, String planCode, String status,
        BigDecimal quotaHa, String currency, BigDecimal baseAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, Instant startsAt, Instant endsAt, boolean entitlementActive) { }
