package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload for redeeming a cooperative invitation code.
 *
 * @param code    the code the cooperative gave the producer
 * @param quotaHa the hectares the producer declared and wants covered
 */
@Schema(
        name = "RedeemCooperativeCodeResource",
        description = "Request payload for redeeming a cooperative invitation code",
        example = "{\"code\": \"COOP-7K2Q\", \"quotaHa\": 12}"
)
public record RedeemCooperativeCodeResource(
        @NotBlank(message = "Code cannot be blank")
        @Schema(description = "Invitation code given by the cooperative, COOP- followed by four letters or digits",
                example = "COOP-7K2Q")
        String code,

        @NotNull(message = "Hectares cannot be null")
        @Schema(description = "Hectares declared by the producer, at least 0.1", example = "12")
        BigDecimal quotaHa
) {
}
