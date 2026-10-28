package com.arcadiadevs.viora.platform.subscription.domain.services;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;

/**
 * When a plan in force can grow and how many months a paid extension bills.
 * <p>In the last {@link #RENEWAL_WINDOW} of a plan, extending it makes no sense: the producer renews instead, buying a
 * new year that starts when the current plan ends. Before that window a paid extension bills the days left until
 * the plan ends, a started day counting as a whole one, at the yearly rate spread over the days of the plan.</p>
 */
public final class PlanExtensionPolicy {

    /** The last stretch of a plan, in which it is renewed instead of extended. */
    public static final Duration RENEWAL_WINDOW = Duration.ofDays(30);

    private static final long SECONDS_PER_DAY = 86_400L;

    private PlanExtensionPolicy() {
    }

    /**
     * Checks whether a plan is in its renewal window at an instant.
     *
     * @param period the plan window
     * @param at     the instant to check
     * @return true if the plan ends within {@link #RENEWAL_WINDOW} of {@code at}
     */
    public static boolean isInRenewalWindow(SubscriptionPeriod period, Instant at) {
        return period != null && at != null && !at.plus(RENEWAL_WINDOW).isBefore(period.endsAt());
    }

    /**
     * Counts the complete months left of a plan, in UTC.
     *
     * @param period the plan window
     * @param at     the instant of the extension
     * @return the complete months between {@code at} and the end of the plan, at least one
     * @throws IllegalArgumentException if an argument is null
     */
    public static int billableMonths(SubscriptionPeriod period, Instant at) {
        if (period == null || at == null) {
            throw new IllegalArgumentException("subscription.extension.months.incomplete");
        }
        var months = ChronoUnit.MONTHS.between(at.atZone(ZoneOffset.UTC), period.endsAt().atZone(ZoneOffset.UTC));
        return (int) Math.max(1, months);
    }

    /**
     * Counts the days left of a plan; a started day counts as a whole one.
     *
     * @param period the plan window
     * @param at     the instant of the extension
     * @return the days between {@code at} and the end of the plan, at least one
     * @throws IllegalArgumentException if an argument is null
     */
    public static int billableDays(SubscriptionPeriod period, Instant at) {
        if (period == null || at == null) {
            throw new IllegalArgumentException("subscription.extension.days.incomplete");
        }
        long seconds = Duration.between(at, period.endsAt()).getSeconds();
        return (int) Math.max(1, (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY);
    }

    /**
     * Counts the days of a plan, so a year of 366 days is spread like one of 365.
     *
     * @param period the plan window
     * @return the days between its start and its end, at least one
     * @throws IllegalArgumentException if the period is null
     */
    public static int periodDays(SubscriptionPeriod period) {
        if (period == null) {
            throw new IllegalArgumentException("subscription.extension.days.incomplete");
        }
        long seconds = Duration.between(period.startsAt(), period.endsAt()).getSeconds();
        return (int) Math.max(1, (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY);
    }
}
