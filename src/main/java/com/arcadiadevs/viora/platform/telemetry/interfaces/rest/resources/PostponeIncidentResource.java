package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NullMarked;

/**
 * REST request body resource to postpone (snooze) an active incident.
 *
 * @param durationHours the number of hours to snooze notifications
 */
@Schema(name = "PostponeIncidentResource", description = "Payload to postpone notifications for an active incident")
@NullMarked
public record PostponeIncidentResource(
        @Schema(description = "Postponement duration in hours", example = "4")
        @NotNull(message = "incident.postpone.duration.invalid")
        @Positive(message = "incident.postpone.duration.invalid")
        Long durationHours
) {
}
