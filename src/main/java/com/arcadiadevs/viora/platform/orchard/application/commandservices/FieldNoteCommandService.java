package com.arcadiadevs.viora.platform.orchard.application.commandservices;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.AttachFieldNotePhotoCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RecordFieldNoteCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service port writing the field notes of the logbook.
 */
public interface FieldNoteCommandService {

    /**
     * Stores a note. Sending the same note again (same identity and content) answers the stored one, so the phone
     * can retry after a lost connection.
     *
     * @param command the note
     * @return the note and whether it was created now, or why it was rejected
     */
    Result<Recording, ApplicationError> handle(RecordFieldNoteCommand command);

    /**
     * Attaches or replaces the photo of a note.
     *
     * @param command the photo
     * @return the note with its photo, or why it was rejected
     */
    Result<FieldNote, ApplicationError> handle(AttachFieldNotePhotoCommand command);

    /**
     * Outcome of storing a note.
     *
     * @param note    the stored note
     * @param created false when the same note was already stored
     */
    record Recording(FieldNote note, boolean created) {
    }
}
