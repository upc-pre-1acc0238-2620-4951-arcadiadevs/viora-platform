package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.RiskReasonView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.MemberPlotDetailResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.MemberPlotResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.RiskReasonResource;

import java.util.List;

/**
 * Assembler from member plots to their public representations.
 */
public final class MemberPlotResourceAssembler {

    private MemberPlotResourceAssembler() {
    }

    /**
     * Builds the representation of a plot on the map and lists.
     *
     * @param view the plot
     * @return the resource
     */
    public static MemberPlotResource toResource(MemberPlotView view) {
        return new MemberPlotResource(view.plotId(), view.plotName(), view.producerId(), view.producerName(),
                view.variety(), view.areaHa(), view.latitude(), view.longitude(), view.geoJson(),
                view.sectorZoneId(), view.sectorZoneName(), view.sectorStatus(), view.proposedSectorName(),
                view.riskLevel(), toResources(view.reasons()), view.overloadPercent(), view.distanceKm(),
                view.evaluatedAt());
    }

    /**
     * Builds the read-only representation of a plot.
     *
     * @param view the plot
     * @return the resource
     */
    public static MemberPlotDetailResource toResource(MemberPlotDetailView view) {
        return new MemberPlotDetailResource(view.plotId(), view.plotName(), view.producerId(), view.producerName(),
                view.producerPhone(), view.variety(), view.areaHa(), view.sectorZoneName(), view.sectorStatus(),
                view.riskLevel(), toResources(view.reasons()), view.overloadPercent(), view.sampledTrees(),
                view.lastSampledOn(), view.preThinningFruitsPerShoot(), view.targetFruitsPerShoot(),
                view.percentageToRemove(), view.windowOpensOn(), view.windowClosesOn(), view.thinningExecutedOn(),
                view.estimatedKgPerHaAfterThinning(), view.bearingIndex(), view.bearingCategory(),
                view.evaluatedAt());
    }

    /**
     * Builds the representation of a reason.
     *
     * @param view the reason; may be null
     * @return the resource, or null
     */
    public static RiskReasonResource toResource(RiskReasonView view) {
        return view == null ? null
                : new RiskReasonResource(view.type(), view.incidentType(), view.value(), view.date());
    }

    private static List<RiskReasonResource> toResources(List<RiskReasonView> views) {
        return views.stream().map(MemberPlotResourceAssembler::toResource).toList();
    }
}
