package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for one member of the manager's license, identified by their user.
 *
 * @param memberId  the requested member
 * @param managerId the signed-in manager
 */
public record GetCooperativeMemberByIdAndManagerIdQuery(ProducerId memberId, ManagerId managerId) {

    /**
     * Compact constructor validating that neither field is null.
     *
     * @param memberId  the requested member
     * @param managerId the signed-in manager
     */
    public GetCooperativeMemberByIdAndManagerIdQuery {
        if (memberId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawMemberId  the raw UUID string of the member
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetCooperativeMemberByIdAndManagerIdQuery(String rawMemberId, String rawManagerId) {
        this(new ProducerId(rawMemberId), new ManagerId(rawManagerId));
    }
}
