package com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A field note of the logbook.
 *
 * @param id         note identity
 * @param plotId     plot it is about
 * @param type       what it is about
 * @param text       free text
 * @param observedOn day it refers to
 * @param recordedAt instant the server stored it
 * @param hasPhoto   whether a photo can be downloaded from {@code /photo}
 */
@Schema(name = "FieldNoteResource", description = "Field note of the logbook")
public record FieldNoteResource(
        String id,
        String plotId,
        @Schema(example = "IRRIGATION") String type,
        String text,
        LocalDate observedOn,
        Instant recordedAt,
        boolean hasPhoto) {
}
