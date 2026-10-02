package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.time.Instant;

/**
 * Seal of a certified dossier: the digest of its stored bytes, the declared signature and the instant.
 *
 * @param verificationHash SHA-256 of the exact stored PDF bytes
 * @param auditorSignature declared collegiate signature
 * @param certifiedAt      single instant of the certification operation
 */
public record DossierMetadata(VerificationHash verificationHash, AuditorSignature auditorSignature,
        Instant certifiedAt) {

    public DossierMetadata {
        if (verificationHash == null || auditorSignature == null || certifiedAt == null) {
            throw new IllegalArgumentException("settlement.certification.metadata.invalid");
        }
    }
}
