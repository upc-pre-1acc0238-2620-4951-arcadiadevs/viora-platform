package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.commands.UpdatePlotCommand;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.UpdatePlotResource;

/**
 * Assembler to convert an UpdatePlotResource and contextual identifiers to an UpdatePlotCommand.
 */
public final class UpdatePlotCommandFromResourceAssembler {

    private UpdatePlotCommandFromResourceAssembler() {
    }

    /**
     * Converts an {@link UpdatePlotResource} and path/header metadata into an {@link UpdatePlotCommand}.
     *
     * @param plotId           the target plot UUID string
     * @param producerId       the managing producer UUID string
     * @param expectedRevision the optimistic locking expected revision
     * @param resource         the incoming HTTP request body
     * @return the domain command
     */
    public static UpdatePlotCommand toCommandFromResource(
            String plotId,
            String producerId,
            long expectedRevision,
            UpdatePlotResource resource
    ) {
        return new UpdatePlotCommand(
                plotId,
                producerId,
                resource.name(),
                resource.rowSpacingM(),
                resource.treeSpacingM(),
                resource.lastPruningDate(),
                resource.polygonGeoJson(),
                expectedRevision
        );
    }
}
