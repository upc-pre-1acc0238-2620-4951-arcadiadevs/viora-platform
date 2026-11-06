package com.arcadiadevs.viora.platform.subscription.application.queryservices;

import java.time.Instant;

/**
 * Read model of a data sharing consent as its producer sees it: with whom they share and since when they stopped.
 * <p>It is not part of the domain: it adds to the consent the organization of the license and the name of its
 * current manager, who is the one reading, kept by the Profiles context.</p>
 *
 * @param id                 the consent identifier
 * @param organizationName   the organization the producer shares with
 * @param managerName        the manager who reads the data now; null while the license has none
 * @param status             {@code ACTIVE}, {@code WITHDRAWN} or {@code ENDED}
 * @param acceptedAt         when the producer accepted
 * @param withdrawnAt        when the producer stopped sharing; null while they share
 * @param planEndsAt         when the sponsored plan, and reading with it, ends, exclusive
 * @param consentTextVersion the version of the text the producer accepted
 */
public record DataSharingConsentView(String id, String organizationName, String managerName, String status,
                                     Instant acceptedAt, Instant withdrawnAt, Instant planEndsAt,
                                     String consentTextVersion) {
}
