package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * REST request body payload for updating a mitigation step.
 *
 * @param completed optional boolean flag indicating completion status
 */
@Schema(name = "CompleteMitigationStepResource", description = "Payload to update mitigation step state")
@NullMarked
public record CompleteMitigationStepResource(
        @Schema(description = "Optional boolean flag to set completion status", example = "true")
        @Nullable Boolean completed
) {
}
