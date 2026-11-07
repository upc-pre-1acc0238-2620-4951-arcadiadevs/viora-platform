package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * Command asking to bring the sector alerts of a license up to date with the colors of its member plots.
 *
 * @param licenseId the license
 */
public record EvaluateSectorAlertsCommand(String licenseId) {

    /**
     * Compact constructor enforcing a non-null license.
     */
    public EvaluateSectorAlertsCommand {
        if (licenseId == null) {
            throw new IllegalArgumentException("territory.license.id.null_or_empty");
        }
    }
}
