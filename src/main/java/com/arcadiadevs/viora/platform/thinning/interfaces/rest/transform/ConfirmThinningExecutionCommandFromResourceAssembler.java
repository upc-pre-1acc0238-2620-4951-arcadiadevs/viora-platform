package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.commands.ConfirmThinningExecutionCommand;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ConfirmExecutionResource;

/** Maps HTTP input to the validated execution command. */
public final class ConfirmThinningExecutionCommandFromResourceAssembler {
    private ConfirmThinningExecutionCommandFromResourceAssembler() { }

    public static ConfirmThinningExecutionCommand toCommand(String id, ConfirmExecutionResource resource) {
        return new ConfirmThinningExecutionCommand(id, resource.executedDate(),
                resource.actualRemovalPercentage(), resource.removedKg(), resource.laborCrewSize(), resource.notes());
    }
}
