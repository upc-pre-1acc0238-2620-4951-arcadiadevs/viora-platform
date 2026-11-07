package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An alert of the manager's alert center about one of their sectors.
 *
 * @param id               the alert
 * @param sectorZoneId     the sector, to open it
 * @param sectorZoneName   the sector's name
 * @param type             {@code SECTOR_CRITICAL} or {@code FROST}
 * @param severity         {@code CRITICAL} or {@code ATTENTION}
 * @param status           {@code ACTIVE} or {@code NORMALIZED}
 * @param openedAt         when it opened
 * @param updatedAt        when its facts last changed
 * @param normalizedAt     when it normalized; null while active
 * @param sectorLevelAfter the sector's color when it normalized; null while active
 * @param affectedPlots    the plots the alert is about
 * @param sectorPlots      the member plots of the sector
 * @param leadPlotId       the plot that leads it; null when its owner no longer shares
 * @param leadPlotName     that plot's name
 * @param leadProducerName its owner's name
 * @param leadReason       why it leads
 * @param leadValue        the overload percentage or the forecast minimum
 * @param leadDate         the day of the forecast minimum
 */
@Schema(name = "SectorAlertResource", description = "Alert about a sector of the manager's license")
public record SectorAlertResource(
        String id,
        String sectorZoneId,
        @Schema(example = "La Yarada Baja") String sectorZoneName,
        @Schema(example = "SECTOR_CRITICAL") String type,
        @Schema(example = "CRITICAL") String severity,
        @Schema(example = "ACTIVE") String status,
        Instant openedAt,
        Instant updatedAt,
        Instant normalizedAt,
        @Schema(example = "MODERATE") String sectorLevelAfter,
        @Schema(example = "5") int affectedPlots,
        @Schema(example = "18") int sectorPlots,
        String leadPlotId,
        @Schema(example = "La Yarada 02") String leadPlotName,
        @Schema(example = "Teodoro Mamani") String leadProducerName,
        @Schema(example = "OVERLOAD") String leadReason,
        @Schema(example = "43.0") Double leadValue,
        LocalDate leadDate
) {
}
