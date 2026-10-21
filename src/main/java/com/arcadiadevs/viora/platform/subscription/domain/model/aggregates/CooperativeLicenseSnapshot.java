package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;

import java.math.BigDecimal;

/**
 * Immutable snapshot of a {@link CooperativeLicense}.
 *
 * @param id               the license identity
 * @param cooperativeId    the cooperative that holds the license
 * @param cooperativeName  the display name of the cooperative
 * @param seats            the number of producers the license covers
 * @param contractedArea   the hectares the license covers in total
 * @param committedAreaHa  the hectares already committed to producers, with two decimals
 * @param period           the window in which the license is in force
 * @param revision         the optimistic locking revision
 */
public record CooperativeLicenseSnapshot(
        CooperativeLicenseId id,
        CooperativeId cooperativeId,
        String cooperativeName,
        int seats,
        HectaresQuota contractedArea,
        BigDecimal committedAreaHa,
        SubscriptionPeriod period,
        Long revision
) {
}
