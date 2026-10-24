package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for redeeming an extension code. The hectares come from the code itself.
 *
 * @param code the extension code shared by the license that sponsors the producer
 */
@Schema(name = "RedeemExtensionCodeResource", description = "Request payload for redeeming an extension code",
        example = "{\"code\": \"AMPL-3R8T\"}")
public record RedeemExtensionCodeResource(
        @NotBlank(message = "Code cannot be blank")
        @Schema(description = "Extension code, AMPL- followed by four letters or digits", example = "AMPL-3R8T")
        String code
) {
}
