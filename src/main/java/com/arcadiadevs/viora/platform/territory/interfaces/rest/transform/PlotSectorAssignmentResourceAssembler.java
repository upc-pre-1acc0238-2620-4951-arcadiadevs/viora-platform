package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.PlotSectorAssignmentView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.PlotSectorAssignmentResource;

/**
 * Assembler from plot sectors to their public representation.
 */
public final class PlotSectorAssignmentResourceAssembler {

    private PlotSectorAssignmentResourceAssembler() {
    }

    /**
     * Builds the representation of a plot sector.
     *
     * @param view the plot sector
     * @return the resource
     */
    public static PlotSectorAssignmentResource toResource(PlotSectorAssignmentView view) {
        return new PlotSectorAssignmentResource(view.plotId(), view.plotName(), view.producerId(),
                view.producerName(), view.status(), view.sectorZoneId(), view.sectorZoneName(), view.proposedName(),
                view.updatedAt());
    }
}
