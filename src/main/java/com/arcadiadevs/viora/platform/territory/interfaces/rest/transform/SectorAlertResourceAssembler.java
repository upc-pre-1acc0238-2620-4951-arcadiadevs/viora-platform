package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorAlertView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorAlertResource;

/**
 * Assembler from sector alerts to their public representation.
 */
public final class SectorAlertResourceAssembler {

    private SectorAlertResourceAssembler() {
    }

    /**
     * Builds the representation of an alert.
     *
     * @param view the alert
     * @return the resource
     */
    public static SectorAlertResource toResource(SectorAlertView view) {
        return new SectorAlertResource(view.id(), view.sectorZoneId(), view.sectorZoneName(), view.type(),
                view.severity(), view.status(), view.openedAt(), view.updatedAt(), view.normalizedAt(),
                view.sectorLevelAfter(), view.affectedPlots(), view.sectorPlots(), view.leadPlotId(),
                view.leadPlotName(), view.leadProducerName(), view.leadReason(), view.leadValue(), view.leadDate());
    }
}
