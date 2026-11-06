package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ConsentTextVersion;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;

import java.time.Instant;

/**
 * Immutable snapshot of a {@link DataSharingConsent}.
 *
 * @param id             the consent identity
 * @param producerId     the producer who accepted
 * @param licenseId      the license whose manager can read
 * @param subscriptionId the sponsored plan the consent came with
 * @param managerId      the manager in charge when the producer accepted; null if the license had none
 * @param textVersion    the version of the text the producer accepted
 * @param acceptedAt     when the producer accepted
 * @param endsAt         when the sponsored plan ends, and reading with it, exclusive
 * @param withdrawnAt    when the producer stopped sharing; null while they share
 * @param revision       the optimistic locking revision
 */
public record DataSharingConsentSnapshot(
        DataSharingConsentId id,
        ProducerId producerId,
        CooperativeLicenseId licenseId,
        SubscriptionId subscriptionId,
        ManagerId managerId,
        ConsentTextVersion textVersion,
        Instant acceptedAt,
        Instant endsAt,
        Instant withdrawnAt,
        Long revision
) {
}
