package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for redeeming a cooperative invitation code.
 *
 * <p>The hectares come from the code itself, so the producer never sends them.</p>
 *
 * @param code               the code the cooperative gave the producer
 * @param consentTextVersion the version of the consent text the producer accepted, as the preview returned it
 */
@Schema(
        name = "RedeemCooperativeCodeResource",
        description = "Request payload for redeeming a cooperative invitation code",
        example = "{\"code\": \"COOP-7K2Q\", \"consentTextVersion\": \"2026-11-v1\"}"
)
public record RedeemCooperativeCodeResource(
        @NotBlank(message = "Code cannot be blank")
        @Schema(description = "Invitation code given by the cooperative, COOP- followed by four letters or digits",
                example = "COOP-7K2Q")
        String code,
        @NotBlank(message = "Consent text version cannot be blank")
        @Schema(description = "Version of the consent text the producer accepted to share their data, as the preview "
                + "returned it", example = "2026-11-v1")
        String consentTextVersion
) {
}
