package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST request payload resource for submitting a field sampling batch.
 *
 * @param clientBatchId client-side UUID for idempotency
 * @param actorId       user identifier of technician or producer
 * @param campaignYear  agricultural campaign year
 * @param samples       list of tree observations
 */
@Schema(
        name = "SubmitSamplingResource",
        description = "Request payload resource for ingesting an olive field sampling batch",
        example = "{\"clientBatchId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"actorId\": \"550e8400-e29b-41d4-a716-446655440000\", \"campaignYear\": 2026, \"samples\": []}"
)
@NullMarked
public record SubmitSamplingResource(
        @NotBlank(message = "thinning.client_batch.id.null_or_empty")
        @Schema(description = "Client-generated idempotency UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        String clientBatchId,

        @NotBlank(message = "thinning.actor.id.null_or_empty")
        @Schema(description = "UUID of the technician or producer taking samples", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        String actorId,

        @NotNull(message = "thinning.campaign_year.null")
        @Min(value = 1980, message = "thinning.campaign_year.invalid")
        @Max(value = 2100, message = "thinning.campaign_year.invalid")
        @Schema(description = "Agricultural campaign year (1980-2100)", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer campaignYear,

        @NotNull(message = "thinning.samples.empty")
        @NotEmpty(message = "thinning.samples.empty")
        @Schema(description = "List of evaluated tree observations", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@Valid TreeSampleResourceItem> samples
) {
}
