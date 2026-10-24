package com.arcadiadevs.viora.platform.orchard.application.queryservices;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;

/**
 * Application service port reading the field notes of the logbook.
 */
public interface FieldNoteQueryService {

    /**
     * Lists the notes of a plot of the producer, newest first. Archived plots keep their notes.
     *
     * @param plotId     the plot
     * @param producerId the producer asking
     * @return the notes, or why they cannot be read
     */
    Result<List<FieldNote>, ApplicationError> findByPlot(String plotId, String producerId);

    /**
     * Reads the photo of a note.
     *
     * @param plotId     plot of the note
     * @param noteId     the note
     * @param producerId the producer asking
     * @return the photo, or not found when the note has none
     */
    Result<FieldNotePhoto, ApplicationError> findPhoto(String plotId, String noteId, String producerId);
}
