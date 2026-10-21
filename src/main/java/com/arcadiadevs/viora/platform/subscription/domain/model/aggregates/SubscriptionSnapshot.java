package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import java.time.Instant;
import java.util.UUID;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Immutable reconstruction and read model. */
public record SubscriptionSnapshot(UUID id, String producerId, String planCode, HectaresQuota quota,
        SubscriptionPrice price, String status, SubscriptionPeriod period, String paymentId, Instant createdAt) { }
