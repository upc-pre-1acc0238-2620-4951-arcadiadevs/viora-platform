package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Declared identity of the professional certifying a dossier.
 *
 * <p>The values are declarations stored with the certification; Settlement does not verify them against any
 * professional registry.</p>
 *
 * @param name      full name of the certifying professional, at most {@value #MAX_NAME_LENGTH} characters
 * @param cipNumber collegiate registration number, at most {@value #MAX_CIP_LENGTH} characters
 */
public record CertifierIdentity(String name, String cipNumber) {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_CIP_LENGTH = 20;

    public CertifierIdentity {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("settlement.certification.certifier.invalid");
        }
        if (cipNumber == null || cipNumber.isBlank()) {
            throw new IllegalArgumentException("settlement.certification.cip.invalid");
        }
        name = name.trim();
        cipNumber = cipNumber.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("settlement.certification.certifier.too_long");
        }
        if (cipNumber.length() > MAX_CIP_LENGTH) {
            throw new IllegalArgumentException("settlement.certification.cip.too_long");
        }
    }
}
