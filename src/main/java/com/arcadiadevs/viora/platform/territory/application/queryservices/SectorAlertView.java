package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model of an alert in the manager's alert center.
 *
 * @param id               the alert
 * @param sectorZoneId     the sector, to open it («Abrir sector»)
 * @param sectorZoneName   the sector's name
 * @param type             {@code SECTOR_CRITICAL} or {@code FROST}
 * @param severity         {@code CRITICAL} («Críticas») or {@code ATTENTION} («Atención»)
 * @param status           {@code ACTIVE} or {@code NORMALIZED}
 * @param openedAt         when it opened
 * @param updatedAt        when its facts last changed
 * @param normalizedAt     when it normalized; null while active
 * @param sectorLevelAfter the sector's color when it normalized («volvió a Moderado»); null while active
 * @param affectedPlots    the plots the alert is about
 * @param sectorPlots      the member plots of the sector
 * @param leadPlotId       the plot that leads it; null when its owner no longer shares
 * @param leadPlotName     that plot's name
 * @param leadProducerName its owner's name
 * @param leadReason       why it leads: {@code OVERLOAD}, {@code CRITICAL_INCIDENT}, {@code FROST_FORECAST} or
 *                         {@code WARNING_INCIDENT}
 * @param leadValue        the overload percentage or the forecast minimum; null for an incident
 * @param leadDate         the day of the forecast minimum; null otherwise
 */
public record SectorAlertView(String id, String sectorZoneId, String sectorZoneName, String type, String severity,
                              String status, Instant openedAt, Instant updatedAt, Instant normalizedAt,
                              String sectorLevelAfter, int affectedPlots, int sectorPlots, String leadPlotId,
                              String leadPlotName, String leadProducerName, String leadReason, Double leadValue,
                              LocalDate leadDate) {
}
