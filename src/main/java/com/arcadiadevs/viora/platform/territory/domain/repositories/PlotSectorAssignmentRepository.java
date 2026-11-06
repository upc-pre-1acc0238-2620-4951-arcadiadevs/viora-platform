package com.arcadiadevs.viora.platform.territory.domain.repositories;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;

import java.util.List;
import java.util.Optional;

/**
 * Repository port of the sectors given to plots.
 */
public interface PlotSectorAssignmentRepository {

    /**
     * Finds the sector of a plot in a license and locks it until the transaction ends.
     *
     * @param licenseId the license
     * @param plotId    the plot
     * @return the assignment, or empty if the plot has no sector in that license
     */
    Optional<PlotSectorAssignment> findByLicenseIdAndPlotIdForUpdate(LicenseId licenseId, PlotId plotId);

    /**
     * Lists the plots of a license.
     *
     * @param licenseId the license
     * @return the assignments
     */
    List<PlotSectorAssignment> findAllByLicenseId(LicenseId licenseId);

    /**
     * Lists the sectors a producer gave their plots in a license.
     *
     * @param licenseId  the license
     * @param producerId the producer
     * @return the assignments
     */
    List<PlotSectorAssignment> findAllByLicenseIdAndProducerId(LicenseId licenseId, ProducerId producerId);

    /**
     * Lists the plots of a license «Por confirmar» with the same written name, ignoring accents and case, locking
     * them until the transaction ends.
     *
     * @param licenseId the license
     * @param name      the written name
     * @return the pending assignments
     */
    List<PlotSectorAssignment> findAllPendingByLicenseIdAndNameForUpdate(LicenseId licenseId, SectorZoneName name);

    /**
     * Stores an assignment.
     *
     * @param assignment the assignment
     * @return the stored assignment
     */
    PlotSectorAssignment save(PlotSectorAssignment assignment);

    /**
     * Removes every sector a producer gave their plots, in any license.
     *
     * @param producerId the producer
     */
    void deleteAllByProducerId(ProducerId producerId);
}
