package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Value object holding what a cooperative license still has to give: its seats and hectares in use and free, and the
 * hectares per code it suggests for the next batch.
 * <p>A seat and the hectares of a code are in use from the moment the code is issued until it expires or is revoked;
 * a redeemed code keeps them for the producer.</p>
 *
 * @param seatsUsed                the seats held by codes still to redeem and by redeemed ones
 * @param seatsFree                the seats left for new codes
 * @param reservedAreaHa           the hectares held, with two decimals
 * @param freeAreaHa               the hectares left for new codes, with two decimals
 * @param suggestedHectaresPerCode the free hectares split among the free seats, rounded down to half a hectare; zero
 *                                 when there is no seat or hectare to give
 */
public record LicenseCapacity(
        int seatsUsed,
        int seatsFree,
        BigDecimal reservedAreaHa,
        BigDecimal freeAreaHa,
        BigDecimal suggestedHectaresPerCode
) {

    /**
     * Validates the capacity.
     *
     * @throws IllegalArgumentException if a value is missing or negative
     */
    public LicenseCapacity {
        if (reservedAreaHa == null || freeAreaHa == null || suggestedHectaresPerCode == null) {
            throw new IllegalArgumentException("subscription.license.capacity.incomplete");
        }
        if (seatsUsed < 0 || seatsFree < 0 || reservedAreaHa.signum() < 0 || freeAreaHa.signum() < 0
                || suggestedHectaresPerCode.signum() < 0) {
            throw new IllegalArgumentException("subscription.license.capacity.negative");
        }
    }
}
