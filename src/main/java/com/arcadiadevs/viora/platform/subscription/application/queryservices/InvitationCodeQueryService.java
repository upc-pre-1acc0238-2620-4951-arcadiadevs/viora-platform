package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetInvitationCodesByManagerIdQuery;

import java.util.List;

/**
 * Application query service port of the invitation codes a technical manager issued.
 */
public interface InvitationCodeQueryService {

    /**
     * Retrieves one group of the codes of the license the manager is in charge of, newest batch first.
     *
     * @param query the query containing the manager and the group
     * @return the codes, empty when the manager is in charge of no license
     */
    List<InvitationCodeView> handle(GetInvitationCodesByManagerIdQuery query);
}
