package com.arcadiadevs.viora.platform.subscription.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
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

    /**
     * Lists the members a cooperative license sponsors now, with whether they share their data with it.
     *
     * @param licenseId the license identifier
     * @return the members; empty for an unknown license
     */
    List<LicenseMember> findLicenseMembers(String licenseId);

    /**
     * Finds the name of the organization that holds a cooperative license.
     *
     * @param licenseId the license identifier
     * @return the organization's name, or empty for an unknown license
     */
    Optional<String> findLicenseOrganizationName(String licenseId);

    /**
     * Lists the cooperative licenses in force now that have a technical manager in charge.
     *
     * @return the license identifiers
     */
    List<String> findManagedLicenseIdsInForce();

    /**
     * A member of a cooperative license as other contexts may know it.
     *
     * @param producerId    the producer's user identifier
     * @param memberSince   when their sponsored plan started
     * @param quotaHectares the hectares the plan covers
     * @param maskedCode    the code they redeemed, with only its last two characters; null if unknown
     * @param sharing       {@code SHARING}, {@code WITHDRAWN} or {@code NOT_SHARING}
     * @param withdrawnAt   when they stopped sharing; null unless {@code WITHDRAWN}
     */
    record LicenseMember(String producerId, Instant memberSince, BigDecimal quotaHectares, String maskedCode,
                         String sharing, Instant withdrawnAt) {
    }
}
