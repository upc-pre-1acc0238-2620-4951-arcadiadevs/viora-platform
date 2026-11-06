package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for redeeming a manager activation code.
 *
 * @param code the code Viora gave the organization for its technical manager
 */
@Schema(
        name = "RedeemManagerCodeResource",
        description = "Request payload for redeeming a manager activation code",
        example = "{\"code\": \"GEST-7K2Q\"}"
)
public record RedeemManagerCodeResource(
        @NotBlank(message = "Code cannot be blank")
        @Schema(description = "Manager activation code, GEST- followed by four letters or digits", example = "GEST-7K2Q")
        String code
) {
}
