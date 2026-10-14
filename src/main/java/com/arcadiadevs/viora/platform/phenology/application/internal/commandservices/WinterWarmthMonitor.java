package com.arcadiadevs.viora.platform.phenology.application.internal.commandservices;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.WinterWarmthEvaluatedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ThermalAnomalyStatus;
import com.arcadiadevs.viora.platform.phenology.domain.services.ChillSeasonEvaluator;
import com.arcadiadevs.viora.platform.phenology.domain.services.ErezDynamicModelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Watches the winter of every active plot for warm spells (US23) and publishes what it finds as a
 * {@link WinterWarmthEvaluatedEvent}, so the alert does not depend on the producer opening the chill screen.
 *
 * <p>It runs only inside the chill season and for {@value #GRACE_DAYS} days after it, so an alert still open at the end
 * of the winter gets closed. It reads the same hourly temperatures and applies the same rule as the chill metric.</p>
 */
@Service
public class WinterWarmthMonitor {

    /** Days after the end of the season during which plots are still evaluated, to close open alerts. */
    static final int GRACE_DAYS = 7;

    private static final Logger LOG = LoggerFactory.getLogger(WinterWarmthMonitor.class);

    private final ExternalOrchardService orchard;
    private final HourlyTemperatureProvider temperatureProvider;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public WinterWarmthMonitor(ExternalOrchardService orchard, HourlyTemperatureProvider temperatureProvider,
            ApplicationEventPublisher publisher, Clock clock) {
        this.orchard = orchard;
        this.temperatureProvider = temperatureProvider;
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * Evaluates every active plot when the date falls in the season or its grace days.
     *
     * @return the plots evaluated
     */
    public int checkAllPlots() {
        var today = LocalDate.now(clock.withZone(HourlyTemperatureProvider.PLOT_TIME_ZONE));
        if (!isMonitored(today)) {
            return 0;
        }
        int evaluated = 0;
        for (String plotId : orchard.findAllActivePlotIds()) {
            try {
                if (checkPlot(new PlotId(plotId), today).isPresent()) {
                    evaluated++;
                }
            } catch (RuntimeException exception) {
                // One plot without weather must not stop the others
                LOG.warn("Winter warmth of plot {} could not be evaluated: {}", plotId, exception.getMessage());
            }
        }
        return evaluated;
    }

    /**
     * Evaluates one plot and publishes the result.
     *
     * @param plotId the plot
     * @param today  local date of the plot
     * @return the published event, or empty when the plot has no location or no weather
     */
    public Optional<WinterWarmthEvaluatedEvent> checkPlot(PlotId plotId, LocalDate today) {
        var centroid = orchard.findPlotCentroid(plotId);
        if (centroid.isEmpty()) {
            return Optional.empty();
        }
        int seasonYear = ChillSeasonEvaluator.seasonYearFor(today);
        var start = ChillSeasonEvaluator.seasonStart(seasonYear);
        var end = ChillSeasonEvaluator.seasonEnd(seasonYear);
        var yesterday = today.minusDays(1);
        var to = yesterday.isBefore(end) ? yesterday : end;
        if (to.isBefore(start)) {
            return Optional.empty();
        }
        var hours = temperatureProvider.fetchHourlyTemperatures(centroid.get()[0], centroid.get()[1], start, to);
        if (hours.isEmpty()) {
            return Optional.empty();
        }
        var evaluation = ChillSeasonEvaluator.evaluate(seasonYear, hours.get(),
                ErezDynamicModelCalculator.DEFAULT_VARIETAL_CHILL_THRESHOLD, today);
        var curve = evaluation.dailyCurve();
        var last = curve.isEmpty() ? null : curve.getLast();
        var event = new WinterWarmthEvaluatedEvent(plotId.plotId(), seasonYear, evaluation.thermalAnomaly().name(),
                evaluation.currentWarmStreakDays(), last == null ? null : last.maxTemperatureCelsius(),
                ThermalAnomalyStatus.WARM_DAY_MAX_TEMPERATURE, evaluation.evaluatedThrough(), clock.instant());
        publisher.publishEvent(event);
        return Optional.of(event);
    }

    /** Inside the chill season of the date, or within its grace days. */
    static boolean isMonitored(LocalDate today) {
        int seasonYear = ChillSeasonEvaluator.seasonYearFor(today);
        var start = ChillSeasonEvaluator.seasonStart(seasonYear);
        var end = ChillSeasonEvaluator.seasonEnd(seasonYear);
        return !today.isBefore(start) && !today.isAfter(end.plusDays(GRACE_DAYS));
    }
}
