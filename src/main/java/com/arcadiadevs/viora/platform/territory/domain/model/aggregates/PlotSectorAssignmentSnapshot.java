package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorAssignmentId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

import java.time.Instant;

/**
 * Persisted state of a {@link PlotSectorAssignment}.
 *
 * @param id           the assignment
 * @param licenseId    the license whose list the sector comes from
 * @param plotId       the plot
 * @param producerId   the producer who owns the plot
 * @param status       {@code ASSIGNED} or {@code PENDING}
 * @param sectorZoneId the sector zone; null while pending
 * @param proposedName the name the producer wrote; null once assigned
 * @param updatedAt    when the sector last changed
 * @param revision     the optimistic lock revision
 */
public record PlotSectorAssignmentSnapshot(PlotSectorAssignmentId id, LicenseId licenseId, PlotId plotId,
                                           ProducerId producerId, PlotSectorStatus status, SectorZoneId sectorZoneId,
                                           SectorZoneName proposedName, Instant updatedAt, Long revision) {
}
