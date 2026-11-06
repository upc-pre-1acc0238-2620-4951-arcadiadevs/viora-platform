package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetAllCooperativeLicensesByManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeLicenseByIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application query service port for reading cooperative licenses.
 */
public interface CooperativeLicenseQueryService {

    /**
     * Retrieves the licenses a technical manager is in charge of, in force or not. A manager holds one at most.
     *
     * @param query the query containing the manager identifier
     * @return the licenses, empty while the manager has not redeemed a code
     */
    List<CooperativeLicense> handle(GetAllCooperativeLicensesByManagerIdQuery query);

    /**
     * Retrieves a license only if the technical manager is in charge of it.
     *
     * @param query the query containing the license and manager identifiers
     * @return the license, or empty for both a missing license and one another manager is in charge of
     */
    Optional<CooperativeLicense> handle(GetCooperativeLicenseByIdAndManagerIdQuery query);

    /**
     * Retrieves a license by its identifier.
     *
     * @param query the query containing the license identifier
     * @return the license, or empty if it does not exist
     */
    Optional<CooperativeLicense> handle(GetCooperativeLicenseByIdQuery query);
}
