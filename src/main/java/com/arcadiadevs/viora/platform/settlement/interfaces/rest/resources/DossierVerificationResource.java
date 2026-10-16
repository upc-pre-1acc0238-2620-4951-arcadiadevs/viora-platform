package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Proof that a PDF matches a dossier stored by Viora (US30 scenario 2): the code was found and these are the data
 * of the plot it belongs to.
 */
@Schema(name = "DossierVerificationResource", description = "A SHA-256 that matches a sealed dossier stored by Viora")
public record DossierVerificationResource(
        @Schema(description = "Always true: an unknown code answers 404") boolean matches,
        @Schema(description = "The verified SHA-256") String verificationHash,
        @Schema(description = "Plot UUID") String plotId,
        @Schema(description = "Plot name", example = "La Yarada 02") String plotName,
        @Schema(description = "Plot area in hectares", example = "2.5") Double areaHectares,
        @Schema(description = "Owner producer UUID") String producerId,
        @Schema(description = "Full name of the holder when it was sealed, null without profile",
                example = "Teodoro Mamani", nullable = true) String holderName,
        @Schema(description = "Sealed campaign", example = "2026") Integer campaignYear,
        @Schema(description = "Instant of the seal") Instant sealedAt,
        @Schema(description = "Pages of the PDF", example = "2") Integer pageCount,
        @Schema(description = "First harvested campaign included", example = "2021") Integer historyFromYear,
        @Schema(description = "Last harvested campaign included", example = "2026") Integer historyToYear
) {
}
