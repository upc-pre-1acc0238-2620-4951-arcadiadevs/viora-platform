package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import java.util.List;

/**
 * Summary projection containing aggregate status counts and the list of incidents.
 *
 * @param activeCount     number of currently active incidents
 * @param criticalCount   number of critical incidents
 * @param warningCount    number of warning incidents
 * @param normalizedCount number of normalized (resolved) incidents
 * @param incidents       the list of matching incident items
 */
public record AgroclimaticIncidentsSummary(
        long activeCount,
        long criticalCount,
        long warningCount,
        long normalizedCount,
        List<AgroclimaticIncidentItem> incidents
) {
    public AgroclimaticIncidentsSummary {
        incidents = (incidents != null) ? List.copyOf(incidents) : List.of();
    }
}
