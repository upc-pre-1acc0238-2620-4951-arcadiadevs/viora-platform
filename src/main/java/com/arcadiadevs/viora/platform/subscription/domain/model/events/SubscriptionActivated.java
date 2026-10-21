package com.arcadiadevs.viora.platform.subscription.domain.model.events;

import java.time.Instant;
import java.util.UUID;
/** Approved initial activation, emitted only once. */
public record SubscriptionActivated(UUID subscriptionId, String producerId, Instant startsAt, Instant endsAt) { }
