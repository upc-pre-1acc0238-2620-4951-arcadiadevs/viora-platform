package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;

/**
 * Enriched application projection combining an incident snapshot with cross-context orchard plot details.
 *
 * @param incident    the incident aggregate snapshot
 * @param plotName    the human-readable plot name
 * @param plotVariety the botanical olive variety name
 */
public record AgroclimaticIncidentItem(
        AgroclimaticIncidentSnapshot incident,
        String plotName,
        String plotVariety
) {
}
