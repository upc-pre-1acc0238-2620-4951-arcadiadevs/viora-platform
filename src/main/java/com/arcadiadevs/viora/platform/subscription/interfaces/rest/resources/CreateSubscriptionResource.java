package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
/** Initial contract selection; the server determines all amounts. */
@Schema(description = "Annual plan selection without a client-controlled payable amount")
public record CreateSubscriptionResource(@NotBlank String planCode,
        @NotNull @Digits(integer = 8, fraction = 6) BigDecimal requestedQuotaHa) { }
