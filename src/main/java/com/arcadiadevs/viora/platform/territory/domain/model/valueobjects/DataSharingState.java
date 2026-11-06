package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * Whether a member lets the manager of their license read their data.
 */
public enum DataSharingState {
    /** They accepted and share now. */
    SHARING,
    /** They stopped sharing; the plan stays. */
    WITHDRAWN,
    /** They redeemed without accepting to share. */
    NOT_SHARING
}
