package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotSamplingState;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PlotSamplingStateResource;

import java.util.List;

/**
 * REST assembler mapping domain {@link PlotSamplingState} records into presentation {@link PlotSamplingStateResource}.
 */
public final class PlotSamplingStateResourceAssembler {

    private PlotSamplingStateResourceAssembler() {
    }

    /**
     * Converts a domain list of plot sampling states to presentation DTO list.
     *
     * @param states domain plot sampling states
     * @return presentation resource list
     */
    public static List<PlotSamplingStateResource> toResourceList(List<PlotSamplingState> states) {
        if (states == null) {
            return List.of();
        }
        return states.stream()
                .map(PlotSamplingStateResourceAssembler::toResource)
                .toList();
    }

    /**
     * Converts a single domain plot sampling state to presentation DTO.
     *
     * @param state domain plot sampling state
     * @return presentation resource
     */
    public static PlotSamplingStateResource toResource(PlotSamplingState state) {
        if (state == null) {
            return null;
        }
        return new PlotSamplingStateResource(
                state.plotId().plotId(),
                state.plotName(),
                state.variety(),
                state.areaHectares(),
                state.campaignYear().value(),
                state.samplingStatus(),
                state.sampledTreesCount(),
                state.treesNeeded(),
                state.isRepresentative()
        );
    }
}
