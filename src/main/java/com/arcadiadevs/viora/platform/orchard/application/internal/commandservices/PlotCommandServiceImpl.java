package com.arcadiadevs.viora.platform.orchard.application.internal.commandservices;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.DuplicatePlotNameException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.OliveVariety;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlantationFrame;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.domain.services.CadastralGeometryService;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation orchestrating plot mutation use cases.
 */
@Service
@Transactional
public class PlotCommandServiceImpl implements PlotCommandService {

    private final PlotRepository plotRepository;
    private final CadastralGeometryService cadastralGeometryService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the PlotCommandServiceImpl with required dependencies.
     *
     * @param plotRepository           the domain plot repository port
     * @param cadastralGeometryService the domain geometry calculation service
     * @param eventPublisher           the Spring application event publisher
     */
    public PlotCommandServiceImpl(
            PlotRepository plotRepository,
            CadastralGeometryService cadastralGeometryService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.plotRepository = plotRepository;
        this.cadastralGeometryService = cadastralGeometryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(DelimitPlotCommand command) {
        try {
            var plotName = new PlotName(command.name());
            var producerId = new ProducerId(command.producerId());
            if (plotRepository.existsByNameAndProducerId(plotName, producerId)) {
                return Result.failure(ApplicationError.conflict("plot", "plot.name.duplicate"));
            }

            var variety = OliveVariety.from(command.variety());
            var frame = new PlantationFrame(command.rowSpacingM(), command.treeSpacingM());
            var geometry = cadastralGeometryService.computeGeometry(command.geometry());

            var plot = Plot.delimit(
                    producerId,
                    plotName,
                    variety,
                    geometry,
                    frame
            );

            var savedPlot = plotRepository.save(plot);

            for (var event : plot.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            plot.clearDomainEvents();

            return Result.success(savedPlot.snapshot().id().plotId());
        } catch (DuplicatePlotNameException ex) {
            return Result.failure(ApplicationError.conflict("plot", ex.getMessage()));
        } catch (InvalidPlotGeometryException ex) {
            return Result.failure(ApplicationError.validationError("geometry", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("plot-creation", ex.getMessage()));
        }
    }
}
