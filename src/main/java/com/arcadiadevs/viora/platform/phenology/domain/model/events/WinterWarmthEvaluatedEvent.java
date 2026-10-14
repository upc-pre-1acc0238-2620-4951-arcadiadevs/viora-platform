package com.arcadiadevs.viora.platform.phenology.domain.model.events;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Integration event published every time the winter of a plot is evaluated for warm spells (US23).
 *
 * <p>The rule is local and thermal, not an El Niño index: a day is warm when its maximum is above
 * {@code warmDayThresholdCelsius}, and a spell is active while more than three consecutive warm days run inside the
 * chill season. Consumers decide what to do with each state; the event only states the facts.</p>
 *
 * @param plotId                     the evaluated plot
 * @param seasonYear                 the winter evaluated
 * @param thermalAnomaly             {@code ACTIVE}, {@code RECORDED} or {@code NONE}
 * @param currentWarmStreakDays      consecutive warm days up to the last evaluated day
 * @param latestMaxTemperatureCelsius maximum of the last evaluated day, or null without data
 * @param warmDayThresholdCelsius    maximum above which a day is warm
 * @param evaluatedThrough           last evaluated day, or null without data
 * @param occurredOn                 instant of the evaluation
 */
public record WinterWarmthEvaluatedEvent(
        String plotId,
        int seasonYear,
        String thermalAnomaly,
        int currentWarmStreakDays,
        Double latestMaxTemperatureCelsius,
        double warmDayThresholdCelsius,
        LocalDate evaluatedThrough,
        Instant occurredOn
) {

    public WinterWarmthEvaluatedEvent {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("phenology.event.plot_id.null");
        }
        if (thermalAnomaly == null || thermalAnomaly.isBlank()) {
            throw new IllegalArgumentException("phenology.event.thermal_anomaly.null");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("phenology.event.occurred_on.null");
        }
    }

    /**
     * Tells whether a warm spell is running now.
     *
     * @return true while the anomaly is active
     */
    public boolean isWarmSpellActive() {
        return "ACTIVE".equals(thermalAnomaly);
    }
}
