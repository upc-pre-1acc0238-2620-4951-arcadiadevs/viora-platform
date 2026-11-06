package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Whether the manager of a license can read a producer's data: {@code ACTIVE} while the producer shares it and the
 * sponsored plan lasts, {@code WITHDRAWN} once the producer stopped sharing, and {@code ENDED} once the plan ended,
 * because reading ends with the sponsorship.
 */
public enum DataSharingConsentStatus {
    ACTIVE,
    WITHDRAWN,
    ENDED
}
