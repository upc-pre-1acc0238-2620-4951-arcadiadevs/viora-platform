package com.arcadiadevs.viora.platform.settlement.domain.model.events;

import java.time.Instant;

/**
 * Published when a campaign dossier is certified. Scalar payload only (published language).
 *
 * @param eventId           unique event identifier, for idempotent consumers
 * @param certificationId   identifier of the stored certification
 * @param reportId          agronomic report of the plot
 * @param plotId            certified plot
 * @param campaignYear      certified campaign
 * @param verificationHash  SHA-256 of the stored PDF bytes, 64 lowercase hexadecimal characters
 * @param auditorSignature  declared collegiate signature
 * @param certifiedAt       instant of the certification
 */
public record AgronomicDossierGeneratedEvent(
        String eventId,
        String certificationId,
        String reportId,
        String plotId,
        int campaignYear,
        String verificationHash,
        String auditorSignature,
        Instant certifiedAt
) {
}
