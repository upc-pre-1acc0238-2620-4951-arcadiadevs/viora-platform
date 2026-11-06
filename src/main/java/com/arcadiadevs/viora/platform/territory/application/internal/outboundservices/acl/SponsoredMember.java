package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A member a license sponsors now, as Subscription tells it, in territorial terms.
 *
 * @param producerId  the member
 * @param memberSince when their sponsored plan started
 * @param quotaHa     the hectares the plan covers
 * @param maskedCode  the code they redeemed, masked; null if unknown
 * @param sharing     whether they share their data
 * @param withdrawnAt when they stopped sharing; null unless withdrawn
 */
public record SponsoredMember(ProducerId producerId, Instant memberSince, BigDecimal quotaHa, String maskedCode,
                              DataSharingState sharing, Instant withdrawnAt) {
}
