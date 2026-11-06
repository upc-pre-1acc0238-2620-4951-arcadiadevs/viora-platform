package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;

/**
 * What the other contexts tell about a member plot today, translated into territorial terms: nothing is copied, it is
 * read each time the traffic light is asked for.
 *
 * @param sampled                   whether the plot has a valid sampling this campaign, so its load is known
 * @param preThinningFruitsPerShoot the load the sampling estimated; null without a valid sampling
 * @param targetFruitsPerShoot      the sustainable load; null without a valid sampling
 * @param thinningExecuted          whether the producer registered the thinning, which settles the overload
 * @param criticalIncidents         the open critical incidents, by type
 * @param warningIncidents          the open warning incidents, by type
 * @param forecastMinimums          the forecast minimum of the coming days
 * @param today                     the day of the evaluation, in the producer's time zone
 */
public record PlotRiskSignals(boolean sampled, Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
                              boolean thinningExecuted, List<String> criticalIncidents,
                              List<String> warningIncidents, List<DailyMinimum> forecastMinimums, LocalDate today) {

    /**
     * Normalizes missing lists to empty ones and requires the day.
     *
     * @throws IllegalArgumentException if today is null
     */
    public PlotRiskSignals {
        if (today == null) {
            throw new IllegalArgumentException("territory.risk.today.null");
        }
        criticalIncidents = criticalIncidents == null ? List.of() : List.copyOf(criticalIncidents);
        warningIncidents = warningIncidents == null ? List.of() : List.copyOf(warningIncidents);
        forecastMinimums = forecastMinimums == null ? List.of() : List.copyOf(forecastMinimums);
    }
}
