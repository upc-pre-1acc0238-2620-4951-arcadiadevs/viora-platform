package com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.RectifyHarvestYieldResource;
import org.jspecify.annotations.Nullable;

/**
 * Assembler converting a {@link RectifyHarvestYieldResource} payload into a domain {@link RectifyHarvestYieldCommand}.
 */
public final class RectifyHarvestYieldCommandFromResourceAssembler {

    private RectifyHarvestYieldCommandFromResourceAssembler() {
    }

    /**
     * Converts route parameters, expected revision, and request payload into a domain command.
     *
     * @param plotId           the target plot identifier from route
     * @param recordId         the target harvest entry identifier from route
     * @param expectedRevision optional optimistic locking revision
     * @param resource         the request payload
     * @return the mapped domain command
     */
    public static RectifyHarvestYieldCommand toCommandFromResource(
            String plotId,
            String recordId,
            @Nullable Long expectedRevision,
            RectifyHarvestYieldResource resource
    ) {
        return new RectifyHarvestYieldCommand(
                plotId,
                recordId,
                resource.totalYieldKg(),
                resource.greenKg(),
                resource.blackKg(),
                resource.notes(),
                expectedRevision
        );
    }
}
