package com.arcadiadevs.viora.platform.subscription.interfaces.acl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Open Host Service (OHS) facade exposed by Subscription to other Bounded Contexts.
 *
 * <p>Exclusively returns Java standard and primitive types.</p>
 */
public interface SubscriptionContextFacade {

    /**
     * Resolves the hectares a producer may manage now: those of the plan in force, only while it gives access.
     *
     * @param producerId producer identifier string
     * @return the hectares of the active plan, or empty when the producer has no plan giving access now
     */
    Optional<BigDecimal> findEntitledQuotaHectares(String producerId);

    /**
     * Finds the cooperative license a technical manager is in charge of, while it is in force.
     *
     * @param managerId the technical manager's user identifier
     * @return the license identifier, or empty if the manager has no license in force
     */
    Optional<String> findLicenseIdInChargeOf(String managerId);

    /**
     * Finds the cooperative license that sponsors a producer's plan, while that plan gives access.
     *
     * @param producerId the producer's user identifier
     * @return the license identifier, or empty if the producer has no sponsored plan in force
     */
    Optional<String> findSponsoringLicenseId(String producerId);

    /**
     * Lists the producers a cooperative license sponsors now.
     *
     * @param licenseId the license identifier
     * @return the producers' user identifiers; empty for an unknown license
     */
    List<String> findSponsoredProducerIds(String licenseId);
}
