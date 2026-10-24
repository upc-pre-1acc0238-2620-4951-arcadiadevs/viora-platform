package com.arcadiadevs.viora.platform.orchard.application.internal.queryservices;

import com.arcadiadevs.viora.platform.orchard.application.queryservices.FieldNoteQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.FieldNoteRepository;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads the field notes of the producer's plots, in any status.
 */
@Service
@Transactional(readOnly = true)
public class FieldNoteQueryServiceImpl implements FieldNoteQueryService {

    private final FieldNoteRepository noteRepository;
    private final PlotRepository plotRepository;

    /**
     * @param noteRepository reads the notes and their photos
     * @param plotRepository checks the plot and its owner
     */
    public FieldNoteQueryServiceImpl(FieldNoteRepository noteRepository, PlotRepository plotRepository) {
        this.noteRepository = noteRepository;
        this.plotRepository = plotRepository;
    }

    @Override
    public Result<List<FieldNote>, ApplicationError> findByPlot(String plotId, String producerId) {
        try {
            var plot = new PlotId(plotId);
            var plotError = checkOwnPlot(plot, new ProducerId(producerId));
            if (plotError != null) {
                return Result.failure(plotError);
            }
            return Result.success(noteRepository.findByPlotId(plot));
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("field-note", exception.getMessage()));
        }
    }

    @Override
    public Result<FieldNotePhoto, ApplicationError> findPhoto(String plotId, String noteId, String producerId) {
        try {
            var plot = new PlotId(plotId);
            var id = new FieldNoteId(noteId);
            var plotError = checkOwnPlot(plot, new ProducerId(producerId));
            if (plotError != null) {
                return Result.failure(plotError);
            }
            return noteRepository.findById(id)
                    .filter(note -> note.snapshot().plotId().equals(plot))
                    .flatMap(note -> noteRepository.findPhoto(id))
                    .<Result<FieldNotePhoto, ApplicationError>>map(Result::success)
                    .orElseGet(() -> Result.failure(ApplicationError.notFound("FieldNotePhoto", id.value())));
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("field-note", exception.getMessage()));
        }
    }

    private ApplicationError checkOwnPlot(PlotId plotId, ProducerId producerId) {
        var plot = plotRepository.findById(plotId);
        if (plot.isEmpty()) {
            return ApplicationError.notFound("Plot", plotId.plotId());
        }
        if (!plot.get().snapshot().producerId().equals(producerId)) {
            return ApplicationError.forbidden("plot", "orchard.field_note.plot.not_owner");
        }
        return null;
    }
}
