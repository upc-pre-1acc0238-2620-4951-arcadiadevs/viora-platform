package com.arcadiadevs.viora.platform.territory.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.territory.domain.exceptions.PlotSectorNotPendingException;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorAssignmentId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

import java.time.Instant;

/**
 * Aggregate root of the sector a member gives one of their plots within the license that sponsors them.
 * <p>It is kept by the license and not on the plot, so an individual producer registers plots as always and
 * reorganizing the list never touches the producer's work. A plot has one sector per license: choosing from the list
 * assigns it at once; writing a sector the list does not have leaves it «Por confirmar» until the manager adds it as
 * a sector or says it is the same as another one.</p>
 */
public class PlotSectorAssignment extends AbstractDomainAggregateRoot<PlotSectorAssignment> {

    private final PlotSectorAssignmentId id;
    private final LicenseId licenseId;
    private final PlotId plotId;
    private final ProducerId producerId;
    private PlotSectorStatus status;
    private SectorZoneId sectorZoneId;
    private SectorZoneName proposedName;
    private Instant updatedAt;
    private final Long revision;

    private PlotSectorAssignment(PlotSectorAssignmentSnapshot snapshot) {
        this.id = snapshot.id();
        this.licenseId = snapshot.licenseId();
        this.plotId = snapshot.plotId();
        this.producerId = snapshot.producerId();
        this.status = snapshot.status();
        this.sectorZoneId = snapshot.sectorZoneId();
        this.proposedName = snapshot.proposedName();
        this.updatedAt = snapshot.updatedAt();
        this.revision = snapshot.revision();
    }

    /**
     * Starts the sector of a plot with no sector in this license yet.
     *
     * @param licenseId  the license that sponsors the producer
     * @param plotId     the plot
     * @param producerId the producer who owns it
     * @param zone       the sector chosen from the list, or null to propose {@code written}
     * @param written    the sector the producer wrote, or null when {@code zone} is given
     * @param at         the instant of the choice
     * @return the assignment, assigned or pending
     * @throws IllegalArgumentException if a part is missing, both or none of {@code zone} and {@code written} are
     *                                  given, or the zone is of another license
     */
    public static PlotSectorAssignment start(LicenseId licenseId, PlotId plotId, ProducerId producerId,
                                             SectorZone zone, SectorZoneName written, Instant at) {
        if (licenseId == null || plotId == null || producerId == null) {
            throw new IllegalArgumentException("territory.plot_sector.creation.incomplete");
        }
        var assignment = new PlotSectorAssignment(new PlotSectorAssignmentSnapshot(new PlotSectorAssignmentId(),
                licenseId, plotId, producerId, PlotSectorStatus.PENDING, null, null, null, 0L));
        assignment.change(zone, written, at);
        return assignment;
    }

    /**
     * Rebuilds an assignment from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the assignment
     * @throws IllegalArgumentException if the snapshot is null, misses a part or mixes both states
     */
    public static PlotSectorAssignment reconstitute(PlotSectorAssignmentSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("territory.plot_sector.snapshot.null");
        }
        var assigned = snapshot.status() == PlotSectorStatus.ASSIGNED && snapshot.sectorZoneId() != null
                && snapshot.proposedName() == null;
        var pending = snapshot.status() == PlotSectorStatus.PENDING && snapshot.sectorZoneId() == null
                && snapshot.proposedName() != null;
        if (snapshot.id() == null || snapshot.licenseId() == null || snapshot.plotId() == null
                || snapshot.producerId() == null || snapshot.updatedAt() == null || !(assigned || pending)) {
            throw new IllegalArgumentException("territory.plot_sector.snapshot.incomplete");
        }
        return new PlotSectorAssignment(snapshot);
    }

    /**
     * Changes the sector the producer gives the plot: a zone of the list, or a name the manager will review.
     *
     * @param zone    the sector chosen from the list, or null to propose {@code written}
     * @param written the sector the producer wrote, or null when {@code zone} is given
     * @param at      the instant of the change
     * @throws IllegalArgumentException if both or none are given, or the zone is of another license
     */
    public void change(SectorZone zone, SectorZoneName written, Instant at) {
        if (at == null || (zone == null) == (written == null)) {
            throw new IllegalArgumentException("territory.plot_sector.choice.one_of");
        }
        if (zone != null) {
            assignTo(zone, at);
            return;
        }
        this.status = PlotSectorStatus.PENDING;
        this.sectorZoneId = null;
        this.proposedName = written;
        this.updatedAt = at;
    }

    /**
     * Resolves a plot «Por confirmar» into a zone of the list: either one the manager just added from what the
     * producer wrote, or an existing one that means the same. The producer's plot does not change.
     *
     * @param zone the zone of the list
     * @param at   the instant of the review
     * @throws IllegalArgumentException      if the zone is null or of another license
     * @throws PlotSectorNotPendingException if the sector is not pending
     */
    public void resolve(SectorZone zone, Instant at) {
        if (status != PlotSectorStatus.PENDING) {
            throw new PlotSectorNotPendingException();
        }
        assignTo(zone, at);
    }

    /**
     * Tells whether this plot waits for the manager's review.
     *
     * @return true while «Por confirmar»
     */
    public boolean isPending() {
        return status == PlotSectorStatus.PENDING;
    }

    /**
     * Returns the current state of the assignment.
     *
     * @return the snapshot
     */
    public PlotSectorAssignmentSnapshot snapshot() {
        return new PlotSectorAssignmentSnapshot(id, licenseId, plotId, producerId, status, sectorZoneId, proposedName,
                updatedAt, revision);
    }

    private void assignTo(SectorZone zone, Instant at) {
        if (zone == null || at == null) {
            throw new IllegalArgumentException("territory.plot_sector.zone.null");
        }
        if (!zone.belongsTo(licenseId)) {
            throw new IllegalArgumentException("territory.plot_sector.zone_of_another_license");
        }
        this.status = PlotSectorStatus.ASSIGNED;
        this.sectorZoneId = zone.snapshot().id();
        this.proposedName = null;
        this.updatedAt = at;
    }
}
