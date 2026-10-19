package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

/**
 * Lifecycle status of a user account.
 */
public enum UserAccountStatus {

    /** Registered, but the email address has not been verified yet: the account cannot sign in. */
    PENDING_VERIFICATION,

    /** The email address is verified and the account can sign in. */
    ACTIVE
}
