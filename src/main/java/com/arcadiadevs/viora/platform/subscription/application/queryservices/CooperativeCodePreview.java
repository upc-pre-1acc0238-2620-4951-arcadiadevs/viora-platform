package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model of what a cooperative code would give a producer, shown before they accept to share their data and
 * redeem it.
 * <p>It is not part of the domain: it combines the code and its license with the name of the manager, kept by the
 * Profiles context, and the hectares the producer registered, kept by the Orchard context, only to answer the screen
 * where the producer decides.</p>
 *
 * @param organizationName   the organization that would cover the plan
 * @param managerName        the manager who would read the producer's data; null while the license has none
 * @param quotaHa            the hectares the code covers
 * @param planEndsAt         when the sponsored plan would end, exclusive: the end of the license
 * @param registeredAreaHa   the hectares the producer already registered in active plots
 * @param exceedsQuota       whether the producer registered more hectares than the code covers
 * @param consentTextVersion the version of the consent text to accept when redeeming
 */
public record CooperativeCodePreview(String organizationName, String managerName, BigDecimal quotaHa,
                                     Instant planEndsAt, BigDecimal registeredAreaHa, boolean exceedsQuota,
                                     String consentTextVersion) {
}
