package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Free text of a field note, trimmed, between 1 and {@value #MAX_LENGTH} characters.
 *
 * @param value the text
 */
public record FieldNoteText(String value) {

    public static final int MAX_LENGTH = 500;

    public FieldNoteText {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("orchard.field_note.text.blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("orchard.field_note.text.too_long");
        }
    }
}
