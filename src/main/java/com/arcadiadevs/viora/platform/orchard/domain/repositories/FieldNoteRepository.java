package com.arcadiadevs.viora.platform.orchard.domain.repositories;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port of the field notes. The photo bytes are stored apart from the note.
 */
public interface FieldNoteRepository {

    void save(FieldNote note);

    /**
     * Stores the note with its new photo, replacing the previous bytes, in one transaction.
     *
     * @param note  the note, already carrying the photo's media type
     * @param photo the photo bytes
     */
    void savePhoto(FieldNote note, FieldNotePhoto photo);

    Optional<FieldNote> findById(FieldNoteId id);

    /**
     * Lists the notes of a plot, newest first (by day, then by the instant they were stored).
     *
     * @param plotId the plot
     * @return its notes
     */
    List<FieldNote> findByPlotId(PlotId plotId);

    Optional<FieldNotePhoto> findPhoto(FieldNoteId id);
}
