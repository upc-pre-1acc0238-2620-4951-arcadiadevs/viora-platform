package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of one dossier certification.
 *
 * @param certificationId UUID string
 */
public record CertificationId(String certificationId) {

    public CertificationId {
        if (certificationId == null || certificationId.isBlank()) {
            throw new IllegalArgumentException("settlement.certification.id.null_or_empty");
        }
        try {
            certificationId = UUID.fromString(certificationId.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("settlement.certification.id.invalid_uuid", e);
        }
    }

    public CertificationId() {
        this(UUID.randomUUID().toString());
    }
}
