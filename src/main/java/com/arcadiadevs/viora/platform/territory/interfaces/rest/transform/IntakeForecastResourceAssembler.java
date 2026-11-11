package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.IntakeForecastResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.SectorIntakeResource;

/**
 * Assembler from the intake of a campaign to its public representation.
 */
public final class IntakeForecastResourceAssembler {

    private IntakeForecastResourceAssembler() {
    }

    /**
     * Builds the representation of an intake.
     *
     * @param view the intake
     * @return the resource
     */
    public static IntakeForecastResource toResource(IntakeForecastView view) {
        return new IntakeForecastResource(view.campaignYear(), view.kind(), view.status(), view.totalTons(),
                view.greenTons(), view.blackTons(), view.unclassifiedTons(), view.plots(), view.sampledPlots(),
                view.estimablePlots(), view.coveragePercent(), view.preliminaryThresholdPercent(),
                view.sectors().stream()
                        .map(sector -> new SectorIntakeResource(sector.sectorZoneId(), sector.sectorZoneName(),
                                sector.sectorStatus(), sector.plots(), sector.sampledPlots(), sector.tons(),
                                sector.greenTons(), sector.blackTons(), sector.unclassifiedTons()))
                        .toList(),
                view.evaluatedAt());
    }
}
