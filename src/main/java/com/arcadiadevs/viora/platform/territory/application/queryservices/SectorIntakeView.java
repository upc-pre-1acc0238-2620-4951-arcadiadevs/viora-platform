package com.arcadiadevs.viora.platform.territory.application.queryservices;

/**
 * Read model of the intake of one sector.
 *
 * @param sectorZoneId   the sector; null for «Por confirmar» or no sector
 * @param sectorZoneName its name
 * @param sectorStatus   {@code ASSIGNED}, {@code PENDING} or {@code NONE}
 * @param plots          the member plots counted there
 * @param sampledPlots   those sampled this campaign, or those that settled a past one
 * @param tons             the projected or settled tons; null when none of its plots can tell
 * @param greenTons        the green part of them; null like tons
 * @param blackTons        the black part of them; null like tons
 * @param unclassifiedTons the part of plots without settled history; null like tons
 */
public record SectorIntakeView(String sectorZoneId, String sectorZoneName, String sectorStatus, int plots,
                               int sampledPlots, Double tons, Double greenTons, Double blackTons,
                               Double unclassifiedTons) {
}
