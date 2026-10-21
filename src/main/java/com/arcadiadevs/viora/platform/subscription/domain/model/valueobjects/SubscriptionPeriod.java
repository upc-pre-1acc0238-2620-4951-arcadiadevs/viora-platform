package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Value object holding the window in which a subscription or a license is in force.
 *
 * @param startsAt the first instant of the window, inclusive
 * @param endsAt   the end of the window, exclusive
 */
public record SubscriptionPeriod(Instant startsAt, Instant endsAt) {

    /**
     * Validates the window.
     *
     * @throws IllegalArgumentException if a bound is null or the window does not end after it starts
     */
    public SubscriptionPeriod {
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("subscription.period.null");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("subscription.period.ends_before_start");
        }
    }

    /**
     * Builds the individual calendar-year term without changing sponsored license periods.
     * @param approvedAt verified individual approval time
     * @return one calendar year in UTC with leap-day adjustment
     * @throws IllegalArgumentException if the approval time is missing
     */
    public static SubscriptionPeriod annual(Instant approvedAt) {
        if (approvedAt == null) {
            throw new IllegalArgumentException("subscription.period.null");
        }
        return new SubscriptionPeriod(approvedAt, approvedAt.atZone(ZoneOffset.UTC).plusYears(1).toInstant());
    }

    /**
     * Checks whether an instant falls inside the window.
     *
     * @param at the instant to check
     * @return true if {@code at} is at or after the start and before the end
     */
    public boolean contains(Instant at) {
        return at != null && !at.isBefore(startsAt) && at.isBefore(endsAt);
    }
}
