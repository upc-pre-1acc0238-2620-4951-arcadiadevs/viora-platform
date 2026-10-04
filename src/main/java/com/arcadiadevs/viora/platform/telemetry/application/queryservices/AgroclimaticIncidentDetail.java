package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;

import java.util.List;

/**
 * Detailed view projection of an agroclimatic incident with mitigation steps and weekly trend.
 *
 * @param incident    the incident aggregate snapshot
 * @param plotName    the human-readable plot name
 * @param plotVariety the botanical olive variety
 * @param weeklyTrend the computed recent observation points
 */
public record AgroclimaticIncidentDetail(
        AgroclimaticIncidentSnapshot incident,
        String plotName,
        String plotVariety,
        List<WeeklyTrendPoint> weeklyTrend
) {
    public AgroclimaticIncidentDetail {
        weeklyTrend = (weeklyTrend != null) ? List.copyOf(weeklyTrend) : List.of();
    }
}
