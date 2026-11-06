package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for previewing a cooperative invitation code before redeeming it.
 *
 * @param code the code the cooperative gave the producer
 */
@Schema(name = "PreviewCooperativeCodeResource",
        description = "Request payload for previewing a cooperative invitation code",
        example = "{\"code\": \"COOP-7K2Q\"}")
public record PreviewCooperativeCodeResource(
        @NotBlank(message = "Code cannot be blank")
        @Schema(description = "Invitation code given by the cooperative", example = "COOP-7K2Q")
        String code
) {
}
