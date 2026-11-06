package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeCodePreviewQuery;

/**
 * Application query service port of the cooperative codes, as the producer who received one sees them.
 */
public interface CooperativeCodeQueryService {

    /**
     * Retrieves what a code would give the producer, without redeeming it. A code that cannot be redeemed answers the
     * same as at redemption, and counts as a failed attempt.
     *
     * @param query the query containing the producer and the code
     * @return the preview, or the reason the code cannot be used
     */
    Result<CooperativeCodePreview, ApplicationError> handle(GetCooperativeCodePreviewQuery query);
}
