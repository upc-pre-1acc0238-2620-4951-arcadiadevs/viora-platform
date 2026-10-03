package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** Declaration of the professional certifying the dossier of one settled campaign. */
@Schema(description = "Certification of a settled campaign; signature, name and CIP are declared text, not a digital signature")
public record CertifyDossierResource(
        @NotNull(message = "{settlement.campaign_year.null}")
        @Min(value = 2000, message = "{settlement.campaign_year.invalid}")
        @Max(value = 2100, message = "{settlement.campaign_year.invalid}")
        @Schema(description = "Settled campaign to certify, 2000 to 2100", example = "2026") Integer campaignYear,
        @NotBlank(message = "{settlement.certification.signature.invalid}")
        @Size(max = 120, message = "{settlement.certification.signature.too_long}")
        @Schema(description = "Declared collegiate signature, up to 120 characters",
                example = "CIP-49120-ING-AGRONOMO-SANCHEZ") String auditorSignature,
        @NotBlank(message = "{settlement.certification.certifier.invalid}")
        @Size(max = 120, message = "{settlement.certification.certifier.too_long}")
        @Schema(description = "Name of the certifying professional, up to 120 characters",
                example = "Agronomist Sanchez") String certifiedBy,
        @NotBlank(message = "{settlement.certification.cip.invalid}")
        @Size(max = 20, message = "{settlement.certification.cip.too_long}")
        @Schema(description = "Collegiate registration number, up to 20 characters", example = "49120")
        String cipNumber,
        @Size(max = 1000, message = "{settlement.certification.notes.too_long}")
        @JsonAlias("certificationNotes")
        @Schema(description = "Optional notes, up to 1000 characters; certificationNotes is accepted as an alias",
                example = "Verified campaign records.", nullable = true) String notes) {
}
