package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

import java.time.Instant;

/**
 * Command asking to renew a cooperative license to a later end, as the organization renews its contract. There is no
 * endpoint for it: the script that grants licenses sends it.
 *
 * @param licenseId the license
 * @param newEndsAt the new end, exclusive
 */
public record RenewCooperativeLicenseCommand(String licenseId, Instant newEndsAt) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public RenewCooperativeLicenseCommand {
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        if (newEndsAt == null) {
            throw new IllegalArgumentException("subscription.license.renewal.incomplete");
        }
    }
}
