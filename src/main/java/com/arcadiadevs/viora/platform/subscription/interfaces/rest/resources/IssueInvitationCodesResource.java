package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload for issuing a batch of invitation codes.
 *
 * @param quantity        how many codes, one per producer, from 1 to 50
 * @param hectaresPerCode the hectares each code covers
 * @param validityDays    how many days the codes can be redeemed: 7, 15 or 30, never past the license
 */
@Schema(
        name = "IssueInvitationCodesResource",
        description = "Request payload for issuing a batch of invitation codes",
        example = "{\"quantity\": 5, \"hectaresPerCode\": 5, \"validityDays\": 30}"
)
public record IssueInvitationCodesResource(
        @NotNull(message = "Quantity is required") @Schema(example = "5") Integer quantity,
        @NotNull(message = "Hectares per code are required") @Schema(example = "5") BigDecimal hectaresPerCode,
        @NotNull(message = "Validity is required") @Schema(example = "30", allowableValues = {"7", "15", "30"})
        Integer validityDays
) {
}
