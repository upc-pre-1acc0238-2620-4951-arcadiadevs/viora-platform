package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * A plot of a member who shares, on the manager's map and lists.
 *
 * @param plotId             the plot
 * @param plotName           its name; only unique per producer
 * @param producerId         its owner
 * @param producerName       the owner's name
 * @param variety            its olive variety
 * @param areaHa             its area in hectares
 * @param latitude           its centroid latitude
 * @param longitude          its centroid longitude
 * @param geoJson            its polygon
 * @param sectorZoneId       its sector; null while «Por confirmar» or without one
 * @param sectorZoneName     the sector's name
 * @param sectorStatus       {@code ASSIGNED}, {@code PENDING} or {@code NONE}
 * @param proposedSectorName what the producer wrote while «Por confirmar»
 * @param riskLevel          {@code CRITICAL}, {@code MODERATE}, {@code NO_DATA} or {@code OPTIMAL}
 * @param reasons            why, the most serious first
 * @param overloadPercent    overload in percent; null without a valid sampling
 * @param distanceKm         distance to the manager; null without their location
 * @param evaluatedAt        when the color was computed
 */
@Schema(name = "MemberPlotResource", description = "Plot of a member who shares, with its sector and color")
public record MemberPlotResource(
        String plotId,
        @Schema(example = "La Yarada 02") String plotName,
        String producerId,
        @Schema(example = "Teodoro Mamani") String producerName,
        @Schema(example = "SEVILLANA") String variety,
        @Schema(example = "2.5") Double areaHa,
        @Schema(example = "-18.05") Double latitude,
        @Schema(example = "-70.25") Double longitude,
        String geoJson,
        String sectorZoneId,
        @Schema(example = "La Yarada Baja") String sectorZoneName,
        @Schema(example = "ASSIGNED") String sectorStatus,
        String proposedSectorName,
        @Schema(example = "CRITICAL") String riskLevel,
        List<RiskReasonResource> reasons,
        @Schema(example = "43.0") Double overloadPercent,
        @Schema(example = "1.2") Double distanceKm,
        Instant evaluatedAt
) {
}
