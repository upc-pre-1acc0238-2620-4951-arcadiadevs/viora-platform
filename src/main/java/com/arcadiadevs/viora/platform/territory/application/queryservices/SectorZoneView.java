package com.arcadiadevs.viora.platform.territory.application.queryservices;

/**
 * Read model of a sector zone of the list: its name and how many member plots are in it.
 *
 * @param id        the sector zone
 * @param name      its name
 * @param plotCount the active plots of sponsored members assigned to it; plots «Por confirmar» do not count
 */
public record SectorZoneView(String id, String name, int plotCount) {
}
