package com.arcadiadevs.viora.platform.subscription.domain.model.events;

import java.time.Instant;

/**
 * Published when a cooperative license is renewed to a later end, so the sponsored plans that ended with it follow.
 *
 * @param licenseId      the license
 * @param previousEndsAt its end before the renewal
 * @param newEndsAt      its new end
 * @param occurredOn     when it was renewed
 */
public record CooperativeLicenseRenewedEvent(String licenseId, Instant previousEndsAt, Instant newEndsAt,
                                             Instant occurredOn) {

    /**
     * Compact constructor enforcing non-null event fields.
     */
    public CooperativeLicenseRenewedEvent {
        if (licenseId == null || previousEndsAt == null || newEndsAt == null || occurredOn == null) {
            throw new IllegalArgumentException("subscription.license.renewal_event.incomplete");
        }
    }
}
