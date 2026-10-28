package com.arcadiadevs.viora.platform.orchard.application.internal.commandservices;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.DuplicatePlotNameException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotAlreadyRemovedException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotNotRemovedException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotRevisionMismatchException;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RemovePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RestorePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.UpdatePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.domain.services.CadastralGeometryService;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Application service implementation orchestrating plot mutation use cases.
 *
 * <p>Creating a plot, enlarging one and restoring an archived one need a plan that gives access now, and the active
 * plots must fit in its hectares. The app checks the same before sending, but only the server can enforce it.</p>
 */
@Service
@Transactional
public class PlotCommandServiceImpl implements PlotCommandService {

    private final PlotRepository plotRepository;
    private final CadastralGeometryService cadastralGeometryService;
    private final ApplicationEventPublisher eventPublisher;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final boolean quotaEnforced;

    /** Hectares a plot may exceed the plan by, to absorb the rounding of areas to two decimals. */
    static final double QUOTA_TOLERANCE_HA = 0.005;

    /**
     * Constructs the PlotCommandServiceImpl with required dependencies.
     *
     * @param plotRepository           the domain plot repository port
     * @param cadastralGeometryService the domain geometry calculation service
     * @param eventPublisher           the Spring application event publisher
     * @param externalSubscriptionService resolves the hectares of the producer's plan
     * @param quotaEnforced            whether the plan and its hectares are enforced; only tests that do not deal
     *                                 with plans turn it off
     */
    public PlotCommandServiceImpl(
            PlotRepository plotRepository,
            CadastralGeometryService cadastralGeometryService,
            ApplicationEventPublisher eventPublisher,
            ExternalSubscriptionService externalSubscriptionService,
            @Value("${viora.orchard.plot-quota.enforced:true}") boolean quotaEnforced
    ) {
        if (plotRepository == null) {
            throw new IllegalArgumentException("plot.repository.null");
        }
        if (cadastralGeometryService == null) {
            throw new IllegalArgumentException("plot.cadastral_geometry_service.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("plot.event_publisher.null");
        }
        if (externalSubscriptionService == null) {
            throw new IllegalArgumentException("plot.external_subscription_service.null");
        }
        this.externalSubscriptionService = externalSubscriptionService;
        this.quotaEnforced = quotaEnforced;
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
            var quotaError = quotaError(producerId, geometry.areaHa(), null);
            if (quotaError.isPresent()) {
                return Result.failure(quotaError.get());
            }

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

    @Override
    public Result<Plot, ApplicationError> handle(UpdatePlotCommand command) {
        try {
            var plotId = new PlotId(command.plotId());
            var producerId = new ProducerId(command.producerId());

            var existingPlotOpt = plotRepository.findById(plotId);
            if (existingPlotOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var plot = existingPlotOpt.get();
            if (plot.snapshot().status() != PlotStatus.ACTIVE
                    || !plot.snapshot().producerId().equals(producerId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var newPlotName = new PlotName(command.name());
            if (!plot.snapshot().name().equals(newPlotName)
                    && plotRepository.existsByNameAndProducerId(newPlotName, producerId)) {
                return Result.failure(ApplicationError.conflict("plot", "plot.name.duplicate"));
            }

            var newVariety = command.variety() == null ? null : OliveVariety.from(command.variety());
            var newFrame = new PlantationFrame(command.rowSpacingM(), command.treeSpacingM());
            var newGeometry = cadastralGeometryService.computeGeometry(command.polygonGeoJson());
            if (newGeometry.areaHa() > plot.snapshot().geometry().areaHa()) {
                var quotaError = quotaError(producerId, newGeometry.areaHa(), plotId);
                if (quotaError.isPresent()) {
                    return Result.failure(quotaError.get());
                }
            }

            plot.update(
                    newPlotName,
                    newVariety,
                    newGeometry,
                    newFrame,
                    command.lastPruningDate(),
                    command.expectedRevision()
            );

            var savedPlot = plotRepository.save(plot);

            for (var event : plot.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            plot.clearDomainEvents();

            return Result.success(savedPlot);
        } catch (PlotRevisionMismatchException ex) {
            return Result.failure(ApplicationError.preconditionFailed("plot", "plot.revision.mismatch"));
        } catch (InvalidPlotGeometryException ex) {
            return Result.failure(ApplicationError.validationError("geometry", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("plot-update", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(RemovePlotCommand command) {
        try {
            var plotId = new PlotId(command.plotId());
            var producerId = new ProducerId(command.producerId());

            var existingPlotOpt = plotRepository.findById(plotId);
            if (existingPlotOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var plot = existingPlotOpt.get();
            if (!plot.snapshot().producerId().equals(producerId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            plot.remove(command.reason());

            var savedPlot = plotRepository.save(plot);

            for (var event : plot.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            plot.clearDomainEvents();

            return Result.success(savedPlot.snapshot().id().plotId());
        } catch (PlotAlreadyRemovedException ex) {
            return Result.failure(ApplicationError.conflict("plot", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("plot-removal", ex.getMessage()));
        }
    }

    @Override
    public Result<Plot, ApplicationError> handle(RestorePlotCommand command) {
        try {
            var plotId = new PlotId(command.plotId());
            var producerId = new ProducerId(command.producerId());

            var existingPlotOpt = plotRepository.findById(plotId);
            if (existingPlotOpt.isEmpty()
                    || !existingPlotOpt.get().snapshot().producerId().equals(producerId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            // An archived plot keeps reserving its name, so restoring it can never clash with another plot.
            var plot = existingPlotOpt.get();
            if (plot.snapshot().status() != PlotStatus.ACTIVE) {
                var quotaError = quotaError(producerId, plot.snapshot().geometry().areaHa(), plotId);
                if (quotaError.isPresent()) {
                    return Result.failure(quotaError.get());
                }
            }
            plot.restore();

            var savedPlot = plotRepository.save(plot);

            for (var event : plot.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            plot.clearDomainEvents();

            return Result.success(savedPlot);
        } catch (PlotNotRemovedException ex) {
            return Result.failure(ApplicationError.conflict("plot", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("plot-restoration", ex.getMessage()));
        }
    }

    /**
     * Checks that the producer has a plan giving access and that the active plots, with this one at its new area,
     * fit in its hectares. The tolerance absorbs the rounding of the area to two decimals, as the app does.
     *
     * @param producerId the owner
     * @param areaHa     the area the plot will have
     * @param plotId     the plot being changed, left out of the hectares already used; null for a new plot
     * @return the error to answer, or empty when it fits
     */
    private Optional<ApplicationError> quotaError(ProducerId producerId, double areaHa, PlotId plotId) {
        if (!quotaEnforced) {
            return Optional.empty();
        }
        var quota = externalSubscriptionService.findEntitledQuotaHectares(producerId.producerId());
        if (quota.isEmpty()) {
            return Optional.of(ApplicationError.forbidden("plot", "plot.subscription.inactive"));
        }
        double usedHa = plotRepository.findActiveByProducerId(producerId).stream()
                .filter(plot -> !plot.snapshot().id().equals(plotId))
                .mapToDouble(plot -> plot.snapshot().geometry().areaHa())
                .sum();
        if (usedHa + areaHa > quota.getAsDouble() + QUOTA_TOLERANCE_HA) {
            return Optional.of(ApplicationError.conflict("plot", "plot.quota.exceeded", Map.of(
                    "quotaHa", quota.getAsDouble(),
                    "usedHa", Math.round(usedHa * 100.0) / 100.0,
                    "requestedHa", areaHa)));
        }
        return Optional.empty();
    }
}
