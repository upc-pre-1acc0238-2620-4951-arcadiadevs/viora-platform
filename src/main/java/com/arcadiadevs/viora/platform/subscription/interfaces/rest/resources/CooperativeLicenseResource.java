package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public representation of the license a technical manager is in charge of.
 *
 * @param id               the license identifier
 * @param organizationName the organization that contracted it
 * @param seats            the producers it covers
 * @param contractedAreaHa the hectares it covers in total
 * @param startsAt         when it comes into force
 * @param endsAt           when it ends, exclusive
 * @param active           whether it is in force now
 */
@Schema(name = "CooperativeLicenseResource", description = "License a technical manager is in charge of")
public record CooperativeLicenseResource(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String id,
        @Schema(example = "Cooperativa Olivar del Sur") String organizationName,
        @Schema(example = "60") int seats,
        @Schema(example = "200.00") BigDecimal contractedAreaHa,
        Instant startsAt,
        Instant endsAt,
        boolean active
) {
}
