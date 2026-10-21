package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.time.Instant;
import java.time.ZoneOffset;
/** Calendar-year entitlement in UTC, with an exclusive upper bound. @param startsAt activation @param endsAt expiry */
public record SubscriptionPeriod(Instant startsAt, Instant endsAt) {
    public SubscriptionPeriod {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("subscription.period.invalid");
        }
    }
    /** @param approvedAt provider-verified approval time @return a calendar year, adjusting leap day */
    public static SubscriptionPeriod annual(Instant approvedAt) {
        if (approvedAt == null) {
            throw new IllegalArgumentException("subscription.approval.required");
        }
        return new SubscriptionPeriod(approvedAt, approvedAt.atZone(ZoneOffset.UTC).plusYears(1).toInstant());
    }
    /** @param at evaluation instant @return whether the interval contains it */
    public boolean contains(Instant at) {
        return !at.isBefore(startsAt) && at.isBefore(endsAt);
    }
}
