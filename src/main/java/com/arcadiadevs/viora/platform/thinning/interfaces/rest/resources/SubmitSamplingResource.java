package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST request payload resource for submitting a field sampling batch.
 *
 * @param clientBatchId client-side UUID for idempotency
 * @param campaignYear  agricultural campaign year
 * @param samples       list of tree observations
 */
@Schema(
        name = "SubmitSamplingResource",
        description = "Request payload resource for ingesting an olive field sampling batch",
        example = """
                {
                  "clientBatchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "campaignYear": 2026,
                  "samples": [
                    {
                      "treeTag": "T-01",
                      "shootCount": 10,
                      "fruitSetCount": 120,
                      "trunkDiameterMm": 165.5,
                      "samplingDate": "2026-09-28"
                    },
                    {
                      "treeTag": "T-02",
                      "shootCount": 12,
                      "fruitSetCount": 140,
                      "trunkDiameterMm": 170.0,
                      "samplingDate": "2026-09-28"
                    }
                  ]
                }
                """
)
@NullMarked
public record SubmitSamplingResource(
        @NotBlank(message = "thinning.client_batch.id.null_or_empty")
        @Schema(description = "Client-generated idempotency UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        String clientBatchId,

        @NotNull(message = "thinning.campaign_year.null")
        @Min(value = 1980, message = "thinning.campaign_year.invalid")
        @Max(value = 2100, message = "thinning.campaign_year.invalid")
        @Schema(description = "Agricultural campaign year (1980-2100)", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer campaignYear,

        @NotNull(message = "thinning.samples.empty")
        @NotEmpty(message = "thinning.samples.empty")
        @ArraySchema(
                schema = @Schema(implementation = TreeSampleResourceItem.class),
                arraySchema = @Schema(description = "List of evaluated tree observations (minimum 5 for statistical representativeness)", requiredMode = Schema.RequiredMode.REQUIRED)
        )
        List<@Valid TreeSampleResourceItem> samples
) {
}
