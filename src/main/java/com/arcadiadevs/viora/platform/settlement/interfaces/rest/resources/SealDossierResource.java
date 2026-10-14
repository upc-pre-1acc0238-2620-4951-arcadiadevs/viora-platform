package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request to seal the dossier of a settled campaign.
 *
 * @param campaignYear settled campaign
 */
@Schema(name = "SealDossierResource", description = "Settled campaign whose agronomic dossier the producer seals")
public record SealDossierResource(
        @NotNull @Schema(description = "Settled campaign, 2000 to 2100", example = "2026") Integer campaignYear) {
}
