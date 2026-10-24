package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * What a field note is about, the chips of the "Nueva nota" sheet (P75).
 */
public enum FieldNoteType {
    IRRIGATION,
    PRUNING,
    FERTILIZATION,
    PESTS,
    OTHER;

    /**
     * Reads a type sent by a client.
     *
     * @param value the type name, case insensitive
     * @return the type
     * @throws IllegalArgumentException if it is blank or unknown
     */
    public static FieldNoteType of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("orchard.field_note.type.invalid");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("orchard.field_note.type.invalid", exception);
        }
    }
}
