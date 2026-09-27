package com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.RecordHarvestYieldResource;

/**
 * Assembler converting a {@link RecordHarvestYieldResource} payload into a domain {@link RecordHarvestYieldCommand}.
 */
public final class RecordHarvestYieldCommandFromResourceAssembler {

    private RecordHarvestYieldCommandFromResourceAssembler() {
    }

    /**
     * Converts route parameters and request payload resource into a domain command.
     *
     * @param plotId   the target plot identifier from route
     * @param resource the request payload
     * @return the mapped domain command
     */
    public static RecordHarvestYieldCommand toCommandFromResource(String plotId, RecordHarvestYieldResource resource) {
        return new RecordHarvestYieldCommand(
                plotId,
                resource.campaignYear(),
                resource.totalYieldKg(),
                resource.greenKg(),
                resource.blackKg(),
                resource.notes()
        );
    }
}
