package com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What a producer learns about a cooperative code before redeeming it, to decide whether to share their data.
 *
 * @param organizationName   the organization that sponsors the plan
 * @param managerName        the manager who would read the data; null while the license has none
 * @param quotaHa            the hectares the code covers
 * @param planEndsAt         when the sponsored plan would end, exclusive
 * @param registeredAreaHa   the hectares the producer has registered in their active plots
 * @param exceedsQuota       whether the registered hectares exceed the quota of the code
 * @param consentTextVersion the version of the consent text to send back when accepting it
 */
@Schema(name = "CooperativeCodePreviewResource", description = "Preview of a cooperative code before redeeming it")
public record CooperativeCodePreviewResource(
        @Schema(example = "Cooperativa Agraria Valle Verde") String organizationName,
        @Schema(example = "Lucía Quispe") String managerName,
        @Schema(example = "5.00") BigDecimal quotaHa,
        Instant planEndsAt,
        @Schema(example = "3.40") BigDecimal registeredAreaHa,
        boolean exceedsQuota,
        @Schema(example = "2026-11-v1") String consentTextVersion
) {
}
