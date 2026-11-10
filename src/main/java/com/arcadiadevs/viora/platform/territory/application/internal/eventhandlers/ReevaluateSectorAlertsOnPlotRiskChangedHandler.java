package com.arcadiadevs.viora.platform.territory.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotRemovedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotRestoredEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotUpdatedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentNormalizedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentRaisedEvent;
import com.arcadiadevs.viora.platform.territory.application.internal.commandservices.SectorAlertEvaluator;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SustainableCropLoadDeterminedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * Re-evaluates the sector alerts of a member's license as soon as the color of one of their plots may have changed
 * (US31): a fruit-set sampling, its crop load or the thinning executed, an agroclimatic incident raised or
 * normalized, and a plot updated, removed or restored.
 *
 * <p>Runs after the source transaction commits and in another thread: the producer's operation is never rolled back
 * nor delayed by the evaluation, which reads every member plot and may wait seconds for the weather. A failure is
 * logged and the {@code SectorAlertScheduler} repairs it on its next run. Changes without an event (the frost
 * forecast, consents, sector assignments) still wait for that scheduler.</p>
 */
@Component
public class ReevaluateSectorAlertsOnPlotRiskChangedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReevaluateSectorAlertsOnPlotRiskChangedHandler.class);

    private final SectorAlertEvaluator sectorAlertEvaluator;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalSubscriptionService externalSubscriptionService;

    /**
     * Creates the handler.
     *
     * @param sectorAlertEvaluator        the serialized evaluation of a license's alerts
     * @param externalOrchardService      the owner of a plot
     * @param externalSubscriptionService the license sponsoring a member
     * @throws IllegalArgumentException if any dependency is null
     */
    public ReevaluateSectorAlertsOnPlotRiskChangedHandler(SectorAlertEvaluator sectorAlertEvaluator,
                                                         ExternalOrchardService externalOrchardService,
                                                         ExternalSubscriptionService externalSubscriptionService) {
        if (sectorAlertEvaluator == null || externalOrchardService == null || externalSubscriptionService == null) {
            throw new IllegalArgumentException("territory.sector_alert_handler.dependency.null");
        }
        this.sectorAlertEvaluator = sectorAlertEvaluator;
        this.externalOrchardService = externalOrchardService;
        this.externalSubscriptionService = externalSubscriptionService;
    }

    /**
     * A fruit-set sampling round was completed on a plot.
     *
     * @param event the sampling round
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SamplingRoundCompletedEvent event) {
        if (event != null) {
            reevaluatePlot(event.plotId());
        }
    }

    /**
     * The sustainable crop load of a plot was determined: its overload is known.
     *
     * @param event the crop load
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(SustainableCropLoadDeterminedEvent event) {
        if (event != null) {
            reevaluatePlot(event.plotId());
        }
    }

    /**
     * The thinning of a plot was executed.
     *
     * @param event the execution
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ThinningExecutionConfirmedEvent event) {
        if (event != null) {
            reevaluatePlot(event.plotId());
        }
    }

    /**
     * An agroclimatic incident was raised on a plot.
     *
     * @param event the incident
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(AgroclimaticIncidentRaisedEvent event) {
        if (event != null) {
            reevaluatePlot(event.plotId());
        }
    }

    /**
     * An agroclimatic incident of a plot went back to normal.
     *
     * @param event the incident
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(AgroclimaticIncidentNormalizedEvent event) {
        if (event != null) {
            reevaluatePlot(event.plotId());
        }
    }

    /**
     * A plot was updated.
     *
     * @param event the update
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PlotUpdatedEvent event) {
        if (event != null) {
            reevaluateProducer(event.producerId());
        }
    }

    /**
     * A plot was removed: it no longer counts in its sector, and it has no active owner any more.
     *
     * @param event the removal
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PlotRemovedEvent event) {
        if (event != null) {
            reevaluateProducer(event.producerId());
        }
    }

    /**
     * A removed plot was restored.
     *
     * @param event the restoration
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PlotRestoredEvent event) {
        if (event != null) {
            reevaluateProducer(event.producerId());
        }
    }

    private void reevaluatePlot(String plotId) {
        try {
            plotIdOf(plotId).flatMap(externalOrchardService::findActivePlotOwner).ifPresent(this::reevaluate);
        } catch (RuntimeException exception) {
            LOG.warn("Sector alerts after a change of plot {} failed: {}", plotId, exception.getMessage());
        }
    }

    private void reevaluateProducer(String producerId) {
        try {
            producerIdOf(producerId).ifPresent(this::reevaluate);
        } catch (RuntimeException exception) {
            LOG.warn("Sector alerts after a change of producer {} failed: {}", producerId, exception.getMessage());
        }
    }

    // A producer outside a sponsored plan belongs to no license: nothing to evaluate.
    private void reevaluate(ProducerId producerId) {
        externalSubscriptionService.findSponsoringLicense(producerId).ifPresent(licenseId ->
                sectorAlertEvaluator.evaluate(licenseId).failure().ifPresent(error ->
                        LOG.warn("Sector alerts of license {} not evaluated: {}", licenseId.licenseId(),
                                error.details())));
    }

    private static Optional<PlotId> plotIdOf(String plotId) {
        try {
            return Optional.of(new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<ProducerId> producerIdOf(String producerId) {
        try {
            return Optional.of(new ProducerId(producerId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
