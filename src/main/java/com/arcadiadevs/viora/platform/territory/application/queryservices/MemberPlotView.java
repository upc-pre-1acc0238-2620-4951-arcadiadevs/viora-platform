package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;
import java.util.List;

/**
 * Read model of a member plot on the manager's map and lists: who owns it, where it is, its sector and its color.
 * Nothing private of the producer is here: no notes, assistant, payments nor their location.
 *
 * @param plotId             the plot
 * @param plotName           its name; only unique per producer
 * @param producerId         its owner
 * @param producerName       the owner's name
 * @param variety            its olive variety
 * @param areaHa             its area in hectares
 * @param latitude           its centroid latitude; null if unknown
 * @param longitude          its centroid longitude; null if unknown
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
public record MemberPlotView(String plotId, String plotName, String producerId, String producerName, String variety,
                             Double areaHa, Double latitude, Double longitude, String geoJson, String sectorZoneId,
                             String sectorZoneName, String sectorStatus, String proposedSectorName, String riskLevel,
                             List<RiskReasonView> reasons, Double overloadPercent, Double distanceKm,
                             Instant evaluatedAt) {
}
