package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.CreatePlotResource;

/**
 * Assembler to convert a CreatePlotResource to a DelimitPlotCommand.
 */
public class CreatePlotCommandFromResourceAssembler {

    /**
     * Converts a CreatePlotResource and effective producerId to a DelimitPlotCommand.
     *
     * @param producerId The effective producer UUID string.
     * @param resource   The {@link CreatePlotResource} resource to convert.
     * @return The {@link DelimitPlotCommand} command that results from the conversion.
     */
    public static DelimitPlotCommand toCommandFromResource(String producerId, CreatePlotResource resource) {
        return new DelimitPlotCommand(
                producerId,
                resource.name(),
                resource.variety(),
                resource.polygonGeoJson(),
                resource.rowSpacingM(),
                resource.treeSpacingM()
        );
    }
}
