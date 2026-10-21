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
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;
import java.time.Instant;

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
 * @param planCode       individual offer code, absent for sponsored subscriptions
 * @param price          individual payment terms, absent for sponsored subscriptions
 * @param paymentId      verified individual payment, absent before approval
 * @param createdAt      individual purchase creation time
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
        Long revision,
        String planCode,
        SubscriptionPrice price,
        String paymentId,
        Instant createdAt
) {
    /**
     * Preserves the existing sponsored snapshot constructor and its consumers.
     * @param id subscription identity
     * @param producerId owner
     * @param mode commercial mode
     * @param status lifecycle state
     * @param quota covered capacity
     * @param period effective period
     * @param cooperativeId sponsor
     * @param licenseId sponsoring license
     * @param redeemedCodeId redeemed invitation
     * @param revision persistence revision
     */
    public SubscriptionSnapshot(SubscriptionId id, ProducerId producerId, PlanMode mode,
            SubscriptionStatus status, HectaresQuota quota, SubscriptionPeriod period,
            CooperativeId cooperativeId, CooperativeLicenseId licenseId, InvitationCodeId redeemedCodeId,
            Long revision) {
        this(id, producerId, mode, status, quota, period, cooperativeId, licenseId, redeemedCodeId,
                revision, null, null, null, null);
    }
}
