package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * A data sharing consent as its producer sees it.
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
@Schema(name = "DataSharingConsentResource", description = "Consent of the producer to share their data")
public record DataSharingConsentResource(
        String id,
        @Schema(example = "Cooperativa Agraria Valle Verde") String organizationName,
        @Schema(example = "Lucía Quispe") String managerName,
        @Schema(example = "ACTIVE") String status,
        Instant acceptedAt,
        Instant withdrawnAt,
        Instant planEndsAt,
        @Schema(example = "2026-11-v1") String consentTextVersion
) {
}
