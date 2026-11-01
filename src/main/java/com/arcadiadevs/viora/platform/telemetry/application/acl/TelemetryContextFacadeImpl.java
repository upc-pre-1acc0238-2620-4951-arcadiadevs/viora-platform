package com.arcadiadevs.viora.platform.telemetry.application.acl;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDeviceSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.services.AgroclimaticThresholdEvaluator;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the Telemetry ACL facade, telling other contexts what Telemetry knows about a plot (coverage,
 * thresholds, open incidents, forecast and nodes) without exposing its aggregates. It only reads what is stored or
 * forecast: it never triggers the virtual node simulation and never raises an incident.
 */
@Service
@Transactional(readOnly = true)
public class TelemetryContextFacadeImpl implements TelemetryContextFacade {

    /** How long a fetched forecast is reused, so asking several questions does not call Open-Meteo each time. */
    static final Duration FORECAST_TTL = Duration.ofMinutes(30);

    /** Window the latest reading is searched in: the week the virtual node fills and keeps. */
    static final Duration READINGS_WINDOW = Duration.ofDays(7);

    /**
     * No physical node sends readings yet: every stored reading comes from the virtual node (observed weather) or
     * the demo seeder, so every linked node is reported as simulated until a measured ingest exists.
     */
    static final boolean NODES_SIMULATED = true;

    private final IoTDeviceRepository deviceRepository;
    private final TelemetrySeriesRepository seriesRepository;
    private final AgroclimaticIncidentRepository incidentRepository;
    private final WeatherForecastProvider weatherForecastProvider;
    private final ExternalOrchardService externalOrchardService;
    private final Clock clock;
    private final AgroclimaticThresholdEvaluator thresholdEvaluator = new AgroclimaticThresholdEvaluator();
    private final Map<String, ForecastOutlook> forecasts = new ConcurrentHashMap<>();

    public TelemetryContextFacadeImpl(
            IoTDeviceRepository deviceRepository,
            TelemetrySeriesRepository seriesRepository,
            AgroclimaticIncidentRepository incidentRepository,
            WeatherForecastProvider weatherForecastProvider,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            Clock clock
    ) {
        this.deviceRepository = deviceRepository;
        this.seriesRepository = seriesRepository;
        this.incidentRepository = incidentRepository;
        this.weatherForecastProvider = weatherForecastProvider;
        this.externalOrchardService = externalOrchardService;
        this.clock = clock;
    }

    @Override
    public TelemetryCoverage findTelemetryCoverage(String plotId, Instant from, Instant to) {
        var id = plotIdOf(plotId);
        if (id.isEmpty()) {
            return new TelemetryCoverage(0, 0);
        }
        int nodes = deviceRepository.findActiveByPlotId(id.get()).size();
        if (from == null || to == null || !from.isBefore(to)) {
            return new TelemetryCoverage(nodes, 0);
        }
        int days = (int) seriesRepository.findReadingsByPlotIdAndDateRange(id.get(), from, to).stream()
                .map(reading -> reading.timestamp().timestamp().atZone(ProducerTimeZone.ZONE).toLocalDate())
                .distinct()
                .count();
        return new TelemetryCoverage(nodes, days);
    }

    @Override
    public ClimateThresholds findClimateThresholds() {
        return new ClimateThresholds(AgroclimaticThresholdEvaluator.HEAT_WARNING_CELSIUS,
                AgroclimaticThresholdEvaluator.HEAT_CRITICAL_CELSIUS,
                AgroclimaticThresholdEvaluator.FROST_WARNING_CELSIUS);
    }

    @Override
    public List<OpenIncident> findOpenIncidents(String plotId) {
        return plotIdOf(plotId)
                .map(id -> incidentRepository.findByPlotId(id).stream()
                        .map(incident -> incident.snapshot())
                        .filter(TelemetryContextFacadeImpl::isOpen)
                        .map(TelemetryContextFacadeImpl::toOpenIncident)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public int countPlotsWithOpenIncident(Collection<String> plotIds, String incidentType) {
        final IncidentType type;
        try {
            type = IncidentType.from(incidentType);
        } catch (IllegalArgumentException exception) {
            return 0;
        }
        if (plotIds == null || plotIds.isEmpty()) {
            return 0;
        }
        var ids = plotIds.stream().map(TelemetryContextFacadeImpl::plotIdOf).flatMap(Optional::stream).toList();
        return (int) incidentRepository.findAllByPlotIds(ids, null, null).stream()
                .map(incident -> incident.snapshot())
                .filter(snapshot -> snapshot.type() == type && isOpen(snapshot))
                .map(AgroclimaticIncidentSnapshot::plotId)
                .distinct()
                .count();
    }

    /**
     * Runs outside a transaction, like the forecast query service: the weather call takes seconds and must not hold
     * a database connection.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<ForecastOutlook> findForecastOutlook(String plotId) {
        var id = plotIdOf(plotId);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        var now = clock.instant();
        var cached = forecasts.get(id.get().plotId());
        if (cached != null && cached.syncedAt().plus(FORECAST_TTL).isAfter(now)) {
            return Optional.of(cached);
        }
        var centroid = externalOrchardService.findPlotCentroid(id.get());
        if (centroid.isEmpty()) {
            return Optional.empty();
        }
        var days = weatherForecastProvider.fetchSevenDayForecast(centroid.get()[0], centroid.get()[1]);
        if (days.isEmpty()) {
            // Not cached: the next question tries again
            return Optional.empty();
        }
        var outlook = new ForecastOutlook(now, days.stream().map(this::toOutlook).toList());
        forecasts.put(id.get().plotId(), outlook);
        return Optional.of(outlook);
    }

    @Override
    public PlotSensors findPlotSensors(String plotId) {
        var id = plotIdOf(plotId);
        if (id.isEmpty()) {
            return new PlotSensors(List.of(), null);
        }
        var now = clock.instant();
        var readings = seriesRepository.findReadingsByPlotIdAndDateRange(id.get(), now.minus(READINGS_WINDOW), now);
        var latest = readings.isEmpty() ? null : readings.getLast();
        var soilMoisture = latest == null ? null : latest.soilMoisture().percentage();
        var nodes = deviceRepository.findActiveByPlotId(id.get()).stream()
                .map(device -> toLinkedNode(device.snapshot(), soilMoisture))
                .toList();
        return new PlotSensors(nodes, latest == null ? null : new LatestReading(latest.timestamp().timestamp(),
                latest.temperature().celsius(), latest.relativeHumidity().percentage(),
                latest.soilMoisture().percentage()));
    }

    /** The same open statuses the incidents summary counts as active. */
    private static boolean isOpen(AgroclimaticIncidentSnapshot snapshot) {
        return snapshot.status() == IncidentStatus.ACTIVE || snapshot.status() == IncidentStatus.SNOOZED;
    }

    private static OpenIncident toOpenIncident(AgroclimaticIncidentSnapshot snapshot) {
        var breach = snapshot.breachInfo();
        return new OpenIncident(snapshot.id().incidentId(), snapshot.type().name(), snapshot.severity().name(),
                breach.currentValue(), breach.thresholdValue(), breach.unit(), snapshot.triggeredAt(),
                snapshot.mitigationSteps().size(),
                (int) snapshot.mitigationSteps().stream().filter(MitigationStepSnapshot::completed).count());
    }

    /** Each day judged by the evaluator itself, the same rule that opens the heat and frost incidents. */
    private ForecastDayOutlook toOutlook(WeatherForecastDay day) {
        double max = day.maxTemperature().celsius();
        double min = day.minTemperature().celsius();
        return new ForecastDayOutlook(day.forecastDate(), max, min,
                thresholdEvaluator.evaluateHeatRisk(max).isPresent(),
                thresholdEvaluator.evaluateFrostRisk(min).isPresent());
    }

    /** A soil probe carries the thresholds of its texture and the hydric risk the evaluator sees in the reading. */
    private LinkedNode toLinkedNode(IoTDeviceSnapshot device, Double soilMoisture) {
        boolean probe = device.type() == DeviceType.SOIL_PROBE;
        String risk = probe && soilMoisture != null
                ? thresholdEvaluator.evaluateHydricRisk(soilMoisture, device.soilTextureType())
                        .map(trigger -> trigger.severity().name()).orElse(null)
                : null;
        return new LinkedNode(device.type().name(), device.depth() == null ? null : device.depth().depthCm(),
                NODES_SIMULATED,
                probe ? AgroclimaticThresholdEvaluator.hydricCriticalThreshold(device.soilTextureType()) : null,
                probe ? AgroclimaticThresholdEvaluator.hydricWarningThreshold(device.soilTextureType()) : null,
                risk);
    }

    private static Optional<PlotId> plotIdOf(String plotId) {
        try {
            return Optional.of(new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
