package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeFilter;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;

/**
 * Domain query record for retrieving one group of the invitation codes of the license a technical manager is in
 * charge of.
 *
 * @param managerId the technical manager identifier value object
 * @param filter    the group of codes
 */
public record GetInvitationCodesByManagerIdQuery(ManagerId managerId, InvitationCodeFilter filter) {

    /**
     * Compact constructor validating that neither managerId nor filter is null.
     *
     * @param managerId the technical manager identifier
     * @param filter    the group of codes
     */
    public GetInvitationCodesByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        if (filter == null) {
            throw new IllegalArgumentException("subscription.invitation_code.filter.null");
        }
    }

    /**
     * Convenience constructor from a raw UUID string.
     *
     * @param rawManagerId the raw UUID string of the technical manager
     * @param filter       the group of codes
     */
    public GetInvitationCodesByManagerIdQuery(String rawManagerId, InvitationCodeFilter filter) {
        this(new ManagerId(rawManagerId), filter);
    }
}
