package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Textual collegiate signature of the auditor who certifies a dossier (for example a CIP-based stamp).
 *
 * <p>Only presence and length are validated: no institutional format is invented. The text alone does not prove
 * the identity of the signer nor provide non-repudiation; it is a declared signature stored with the dossier.</p>
 *
 * @param value non-blank signature text, trimmed, at most {@value #MAX_LENGTH} characters
 */
public record AuditorSignature(String value) {

    public static final int MAX_LENGTH = 120;

    public AuditorSignature {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("settlement.certification.signature.invalid");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("settlement.certification.signature.too_long");
        }
    }
}
