package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Paid extension selection; the server prices it for the months the plan has left.
 *
 * @param addedHa the hectares to add, in half-hectare steps
 */
@Schema(description = "Hectares to add to an individual plan in force, without a client-controlled amount",
        example = "{\"addedHa\": 3}")
public record RequestQuotaExtensionResource(@NotNull @Digits(integer = 8, fraction = 6) BigDecimal addedHa) { }
