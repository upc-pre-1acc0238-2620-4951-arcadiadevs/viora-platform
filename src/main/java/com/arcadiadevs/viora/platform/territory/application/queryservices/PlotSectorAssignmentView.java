package com.arcadiadevs.viora.platform.territory.application.queryservices;

import java.time.Instant;

/**
 * Read model of the sector of a member plot, naming the plot and its owner, kept by Orchard and Profiles.
 *
 * @param plotId         the plot
 * @param plotName       its name; it is only unique per producer
 * @param producerId     its owner
 * @param producerName   the owner's name; null without a profile
 * @param status         {@code ASSIGNED} or {@code PENDING}
 * @param sectorZoneId   the zone; null while pending
 * @param sectorZoneName the zone's current name; null while pending
 * @param proposedName   what the producer wrote; null once assigned
 * @param updatedAt      when the sector last changed
 */
public record PlotSectorAssignmentView(String plotId, String plotName, String producerId, String producerName,
                                       String status, String sectorZoneId, String sectorZoneName,
                                       String proposedName, Instant updatedAt) {
}
