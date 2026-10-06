package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Date on which the delivered olives of a campaign were weighed.
 *
 * <p>Harvest is weighed when it is delivered, so the date cannot be in the future. The comparison reads "today" from
 * the injected application {@link Clock}, exactly like {@code HarvestCampaignYearPolicy} does for campaign years:
 * no second clock and no hardcoded zone is introduced here, which keeps the rule deterministic under test.</p>
 *
 * <p>The canonical constructor only rejects a missing date. The "not in the future" rule needs the clock, so it
 * belongs to {@link #of(LocalDate, Clock)} and therefore is not replayed when a stored settlement is read back:
 * a historical weighing date must always reload.</p>
 *
 * @param value the weighing date
 */
public record WeighingDate(LocalDate value) {

    /**
     * Builds a weighing date and rejects one that lies in the future.
     *
     * @param date  the weighing date; today is accepted, tomorrow is not
     * @param clock the clock used to resolve the current date
     * @return the weighing date
     * @throws IllegalArgumentException if the date or the clock is missing, or if the date is after the current
     *                                  date of the clock
     */
    public static WeighingDate of(LocalDate date, Clock clock) {
        if (date == null || clock == null) {
            throw new IllegalArgumentException("settlement.weighed_on.null");
        }
        if (date.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("settlement.weighed_on.future");
        }
        return new WeighingDate(date);
    }

    /**
     * Rebuilds a weighing date from persistence.
     *
     * @param value the persisted weighing date
     * @throws IllegalArgumentException if the date is missing
     */
    public WeighingDate {
        if (value == null) {
            throw new IllegalArgumentException("settlement.weighed_on.null");
        }
    }
}
