package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Optional free-text notes attached to a dossier certification.
 *
 * <p>Null or blank text means that there are no notes; otherwise the text is trimmed and limited in length. This
 * is the single place where the rule lives.</p>
 *
 * @param value trimmed notes of at most {@value #MAX_LENGTH} characters, or {@code null} when absent
 * @throws IllegalArgumentException if the trimmed text is longer than {@value #MAX_LENGTH} characters
 */
public record CertificationNotes(String value) {

    public static final int MAX_LENGTH = 1000;

    public CertificationNotes {
        if (value == null || value.isBlank()) {
            value = null;
        } else {
            value = value.trim();
            if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("settlement.certification.notes.too_long");
            }
        }
    }

    /**
     * Notes of a certification that has none.
     *
     * @return the absent notes
     */
    public static CertificationNotes none() {
        return new CertificationNotes(null);
    }

    /**
     * Tells whether there is any text.
     *
     * @return {@code true} when notes were provided
     */
    public boolean isPresent() {
        return value != null;
    }
}
