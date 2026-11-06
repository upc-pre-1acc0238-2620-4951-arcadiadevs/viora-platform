package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorZoneView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorZoneResource;

/**
 * Assembler from sector zones to their public representation.
 */
public final class SectorZoneResourceAssembler {

    private SectorZoneResourceAssembler() {
    }

    /**
     * Builds the representation of a sector.
     *
     * @param view the sector
     * @return the resource
     */
    public static SectorZoneResource toResource(SectorZoneView view) {
        return new SectorZoneResource(view.id(), view.name(), view.plotCount());
    }
}
