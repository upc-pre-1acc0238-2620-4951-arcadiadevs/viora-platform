package com.arcadiadevs.viora.platform.subscription.domain.model.commands;

/**
 * Command a manager sends to release a member from their license: the manager stops reading at once, the member
 * keeps the plan through a grace period, and then the seat goes back to the license.
 *
 * @param managerId the authenticated technical manager
 * @param licenseId the license
 * @param memberId  the member's user identifier
 */
public record ReleaseCooperativeMemberCommand(String managerId, String licenseId, String memberId) {

    /**
     * Compact constructor enforcing non-null fields.
     */
    public ReleaseCooperativeMemberCommand {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        if (licenseId == null) {
            throw new IllegalArgumentException("subscription.license.id.null_or_empty");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }
}
