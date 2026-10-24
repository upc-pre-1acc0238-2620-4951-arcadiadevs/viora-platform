package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identity of a field note. The phone generates it when the note is written, so sending the same note again after a
 * lost connection never creates a second one.
 *
 * @param value lowercase UUID
 */
public record FieldNoteId(String value) {

    public FieldNoteId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("orchard.field_note.id.invalid");
        }
        try {
            value = UUID.fromString(value.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("orchard.field_note.id.invalid", exception);
        }
    }
}
