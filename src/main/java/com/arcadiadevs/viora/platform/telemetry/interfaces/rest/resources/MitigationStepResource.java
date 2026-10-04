package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * REST presentation resource for a mitigation task step within an incident.
 */
@Schema(name = "MitigationStepResource", description = "Actionable mitigation task item")
@NullMarked
public record MitigationStepResource(
        @Schema(description = "Step unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String id,

        @Schema(description = "i18n translation key describing the mitigation instruction", example = "heat_wave.step.pre_irrigation")
        String instructionKey,

        @Schema(description = "Adaptive, localized human-readable instruction text", example = "Riega el miércoles por la tarde")
        String instruction,

        @Schema(description = "Whether this step has been completed", example = "false")
        boolean completed,

        @Schema(description = "Timestamp when completed, or null if pending", example = "2026-02-18T09:30:00Z")
        @Nullable Instant completedAt
) {
}
