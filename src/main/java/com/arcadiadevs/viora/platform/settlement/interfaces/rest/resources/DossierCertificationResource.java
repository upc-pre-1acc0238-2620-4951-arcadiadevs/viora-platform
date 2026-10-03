package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Stored certification of a campaign dossier. */
@Schema(description = "Certified campaign dossier; the hash is the SHA-256 of the exact stored PDF bytes")
public record DossierCertificationResource(
        String certificationId,
        String reportId,
        String plotId,
        Integer campaignYear,
        @Schema(description = "64 lowercase hexadecimal characters, computed over the stored PDF bytes",
                example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        String verificationHash,
        String auditorSignature,
        String certifiedBy,
        String cipNumber,
        Instant certifiedAt) {
}
