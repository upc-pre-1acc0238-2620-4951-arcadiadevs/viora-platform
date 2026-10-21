package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;

/**
 * Immutable snapshot of a {@link Subscription}.
 *
 * @param id             the subscription identity
 * @param producerId     the producer who holds it
 * @param mode           individual or sponsored by a cooperative
 * @param status         the lifecycle status
 * @param quota          the hectares covered
 * @param period         the window in which it is in force
 * @param cooperativeId  the sponsoring cooperative; set only for the sponsored mode
 * @param licenseId      the sponsoring license; set only for the sponsored mode
 * @param redeemedCodeId the code that activated it; set only for the sponsored mode
 * @param revision       the optimistic locking revision
 */
public record SubscriptionSnapshot(
        SubscriptionId id,
        ProducerId producerId,
        PlanMode mode,
        SubscriptionStatus status,
        HectaresQuota quota,
        SubscriptionPeriod period,
        CooperativeId cooperativeId,
        CooperativeLicenseId licenseId,
        InvitationCodeId redeemedCodeId,
        Long revision
) {
}
