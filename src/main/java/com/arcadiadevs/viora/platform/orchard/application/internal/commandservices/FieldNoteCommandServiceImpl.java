package com.arcadiadevs.viora.platform.orchard.application.internal.commandservices;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.FieldNoteCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.AttachFieldNotePhotoCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RecordFieldNoteCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteText;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteType;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.FieldNoteRepository;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Writes the field notes: only on an active plot of the producer, idempotent by the note identity.
 */
@Service
@Transactional
public class FieldNoteCommandServiceImpl implements FieldNoteCommandService {

    private final FieldNoteRepository noteRepository;
    private final PlotRepository plotRepository;
    private final Clock clock;

    /**
     * @param noteRepository stores the notes and their photos
     * @param plotRepository checks the plot and its owner
     * @param clock          dates the notes and rejects future days
     */
    public FieldNoteCommandServiceImpl(FieldNoteRepository noteRepository, PlotRepository plotRepository, Clock clock) {
        this.noteRepository = noteRepository;
        this.plotRepository = plotRepository;
        this.clock = clock;
    }

    @Override
    public Result<Recording, ApplicationError> handle(RecordFieldNoteCommand command) {
        try {
            var id = new FieldNoteId(command.noteId());
            var plotId = new PlotId(command.plotId());
            var producerId = new ProducerId(command.producerId());
            var type = FieldNoteType.of(command.type());
            var text = new FieldNoteText(command.text());

            var plotError = checkActivePlot(plotId, producerId);
            if (plotError != null) {
                return Result.failure(plotError);
            }

            var stored = noteRepository.findById(id);
            if (stored.isPresent()) {
                if (stored.get().sameAs(plotId, type, text, command.observedOn())) {
                    return Result.success(new Recording(stored.get(), false));
                }
                return Result.failure(ApplicationError.conflict("field-note", "orchard.field_note.id.taken"));
            }

            var note = FieldNote.record(id, plotId, producerId, type, text, command.observedOn(),
                    LocalDate.now(clock), clock.instant());
            noteRepository.save(note);
            return Result.success(new Recording(note, true));
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("field-note", exception.getMessage()));
        }
    }

    @Override
    public Result<FieldNote, ApplicationError> handle(AttachFieldNotePhotoCommand command) {
        try {
            var id = new FieldNoteId(command.noteId());
            var plotId = new PlotId(command.plotId());
            var producerId = new ProducerId(command.producerId());

            var plotError = checkActivePlot(plotId, producerId);
            if (plotError != null) {
                return Result.failure(plotError);
            }
            var stored = noteRepository.findById(id).filter(note -> note.snapshot().plotId().equals(plotId));
            if (stored.isEmpty()) {
                return Result.failure(ApplicationError.notFound("FieldNote", id.value()));
            }

            var photo = FieldNotePhoto.of(command.content());
            var note = stored.get().attachPhoto(photo);
            noteRepository.savePhoto(note, photo);
            return Result.success(note);
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("field-note", exception.getMessage()));
        }
    }

    /** Null when the plot is active and belongs to the producer; otherwise why the note cannot be written. */
    private ApplicationError checkActivePlot(PlotId plotId, ProducerId producerId) {
        var plot = plotRepository.findById(plotId)
                .filter(found -> found.snapshot().status() == PlotStatus.ACTIVE);
        if (plot.isEmpty()) {
            return ApplicationError.notFound("Plot", plotId.plotId());
        }
        if (!plot.get().snapshot().producerId().equals(producerId)) {
            return ApplicationError.forbidden("plot", "orchard.field_note.plot.not_owner");
        }
        return null;
    }
}
