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
 * @param seatsUsed        the seats held by codes still to redeem and by redeemed ones
 * @param seatsFree        the seats left for new codes
 * @param reservedAreaHa   the hectares held by those codes
 * @param freeAreaHa       the hectares left for new codes
 * @param suggestedHectaresPerCode the free hectares split among the free seats, rounded down to half a hectare
 */
@Schema(name = "CooperativeLicenseResource", description = "License a technical manager is in charge of")
public record CooperativeLicenseResource(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String id,
        @Schema(example = "Cooperativa Olivar del Sur") String organizationName,
        @Schema(example = "60") int seats,
        @Schema(example = "200.00") BigDecimal contractedAreaHa,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        @Schema(example = "48") int seatsUsed,
        @Schema(example = "12") int seatsFree,
        @Schema(example = "138.00") BigDecimal reservedAreaHa,
        @Schema(example = "62.00") BigDecimal freeAreaHa,
        @Schema(example = "5.0") BigDecimal suggestedHectaresPerCode
) {
}
