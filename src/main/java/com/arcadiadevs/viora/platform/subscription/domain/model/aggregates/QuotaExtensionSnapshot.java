package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionSource;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;

import java.time.Instant;

/**
 * Immutable snapshot of a {@link QuotaExtension}.
 *
 * @param id             the extension identity
 * @param subscriptionId the plan that grows
 * @param producerId     the producer who holds the plan
 * @param source         a payment or an extension code
 * @param addedArea      the hectares added
 * @param status         the lifecycle status
 * @param billedMonths   the months the payment covers; null for a code
 * @param price          the payment terms; null for a code
 * @param extensionCodeId the redeemed code; null for a payment
 * @param paymentId      the approved payment; null until approved or for a code
 * @param createdAt      when the extension was requested
 * @param appliedAt      when the hectares were added; null while pending
 * @param revision       the optimistic locking revision
 */
public record QuotaExtensionSnapshot(
        QuotaExtensionId id,
        SubscriptionId subscriptionId,
        ProducerId producerId,
        QuotaExtensionSource source,
        HectaresQuota addedArea,
        QuotaExtensionStatus status,
        Integer billedMonths,
        SubscriptionPrice price,
        ExtensionCodeId extensionCodeId,
        String paymentId,
        Instant createdAt,
        Instant appliedAt,
        Long revision
) {
}
