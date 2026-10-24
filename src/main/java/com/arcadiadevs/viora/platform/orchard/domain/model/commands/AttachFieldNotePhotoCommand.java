package com.arcadiadevs.viora.platform.orchard.domain.model.commands;

/**
 * Command to attach (or replace) the photo of a field note.
 *
 * @param noteId     the note
 * @param plotId     plot of the note, from the route
 * @param producerId producer uploading it
 * @param content    the image bytes
 */
public record AttachFieldNotePhotoCommand(
        String noteId,
        String plotId,
        String producerId,
        byte[] content
) {
}
