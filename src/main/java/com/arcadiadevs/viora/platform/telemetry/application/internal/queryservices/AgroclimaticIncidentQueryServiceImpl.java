package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.*;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Implementation of {@link AgroclimaticIncidentQueryService} orchestrating read queries for incidents.
 */
@Service
@Transactional(readOnly = true)
public class AgroclimaticIncidentQueryServiceImpl implements AgroclimaticIncidentQueryService {

    private static final int TREND_DAYS = 7;
    /** Local zone of the plots, where a day starts and ends. */
    private static final ZoneId PLOT_ZONE = ZoneId.of("America/Lima");

    private final AgroclimaticIncidentRepository incidentRepository;
    private final ExternalOrchardService externalOrchardService;
    private final TelemetrySeriesRepository telemetrySeriesRepository;
    private final ExternalPhenologyService externalPhenologyService;

    /**
     * Constructs the AgroclimaticIncidentQueryServiceImpl with required dependencies.
     *
     * @param incidentRepository        the incident domain repository
     * @param externalOrchardService    the outbound ACL service for orchard lookups
     * @param telemetrySeriesRepository the telemetry readings repository
     * @param externalPhenologyService outbound ACL reading the daily maxima behind a warm winter alert
     */
    public AgroclimaticIncidentQueryServiceImpl(
            AgroclimaticIncidentRepository incidentRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            TelemetrySeriesRepository telemetrySeriesRepository,
            @Qualifier("telemetryExternalPhenologyService") ExternalPhenologyService externalPhenologyService
    ) {
        if (incidentRepository == null) {
            throw new IllegalArgumentException("incident.repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        if (telemetrySeriesRepository == null) {
            throw new IllegalArgumentException("telemetry.series_repository.null");
        }
        this.incidentRepository = incidentRepository;
        this.externalOrchardService = externalOrchardService;
        this.telemetrySeriesRepository = telemetrySeriesRepository;
        this.externalPhenologyService = externalPhenologyService;
    }

    @Override
    public Result<AgroclimaticIncidentsSummary, ApplicationError> handle(GetAgroclimaticIncidentsQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query.null");
        }

        if (query.plotId() != null && !externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("Plot", query.plotId().plotId()));
        }

        var matchingIncidents = incidentRepository.findAll(query.plotId(), query.status(), query.severity());
        var allForPlotOrGlobal = incidentRepository.findAll(query.plotId(), null, null);

        long activeCount = allForPlotOrGlobal.stream()
                .filter(i -> i.snapshot().status() == IncidentStatus.ACTIVE || i.snapshot().status() == IncidentStatus.SNOOZED)
                .count();
        long criticalCount = allForPlotOrGlobal.stream()
                .filter(i -> (i.snapshot().status() == IncidentStatus.ACTIVE || i.snapshot().status() == IncidentStatus.SNOOZED)
                        && i.snapshot().severity() == IncidentSeverity.CRITICAL)
                .count();
        long warningCount = allForPlotOrGlobal.stream()
                .filter(i -> (i.snapshot().status() == IncidentStatus.ACTIVE || i.snapshot().status() == IncidentStatus.SNOOZED)
                        && i.snapshot().severity() == IncidentSeverity.WARNING)
                .count();
        long normalizedCount = allForPlotOrGlobal.stream()
                .filter(i -> i.snapshot().status() == IncidentStatus.NORMALIZED)
                .count();

        List<AgroclimaticIncidentItem> items = new ArrayList<>();
        for (AgroclimaticIncident incident : matchingIncidents) {
            var plotId = incident.snapshot().plotId();
            String plotName = externalOrchardService.findPlotName(plotId).orElse("Lote " + plotId.plotId().substring(0, 8));
            String plotVariety = externalOrchardService.findPlotVariety(plotId).orElse("SEVILLANA");

            items.add(new AgroclimaticIncidentItem(incident.snapshot(), plotName, plotVariety));
        }

        return Result.success(new AgroclimaticIncidentsSummary(
                activeCount,
                criticalCount,
                warningCount,
                normalizedCount,
                items
        ));
    }

    @Override
    public Result<AgroclimaticIncidentDetail, ApplicationError> handle(GetAgroclimaticIncidentByIdQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query.null");
        }

        var incidentOpt = incidentRepository.findById(query.incidentId());
        if (incidentOpt.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Incident", query.incidentId().incidentId()));
        }

        var incident = incidentOpt.get();
        var snapshot = incident.snapshot();
        var plotId = snapshot.plotId();

        String plotName = externalOrchardService.findPlotName(plotId).orElse("Lote " + plotId.plotId().substring(0, 8));
        String plotVariety = externalOrchardService.findPlotVariety(plotId).orElse("SEVILLANA");

        // Weekly trend: one point per day (the 24 h windows of the week up to the trigger)
        Instant end = snapshot.triggeredAt();
        Instant start = end.minus(Duration.ofDays(TREND_DAYS));
        List<HourlyTelemetryReadingSnapshot> readings = telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(plotId, start, end);

        List<WeeklyTrendPoint> trendPoints = new ArrayList<>();
        Double threshold = snapshot.breachInfo().thresholdValue();

        if (snapshot.type() == IncidentType.WARM_WINTER) {
            // The same daily maxima the warm winter rule was evaluated on, the week up to the trigger
            trendPoints.addAll(warmWinterTrend(plotId, end, threshold));
        }
        if (trendPoints.isEmpty() && readings != null && !readings.isEmpty()) {
            trendPoints.addAll(dailyExtremes(readings, snapshot.type(), start, threshold));
        }
        if (trendPoints.isEmpty()) {
            // Synthesize single observation point at trigger time
            trendPoints.add(new WeeklyTrendPoint(snapshot.triggeredAt(), snapshot.breachInfo().currentValue(), threshold));
        }

        return Result.success(new AgroclimaticIncidentDetail(snapshot, plotName, plotVariety, trendPoints));
    }

    /**
     * Daily maxima of the week up to the trigger, read from Phenology: the same days the warm winter rule saw.
     */
    private List<WeeklyTrendPoint> warmWinterTrend(PlotId plotId, Instant triggeredAt, Double threshold) {
        // The rule is evaluated up to the day before, the last one the observed weather covers in full
        var lastDay = triggeredAt.atZone(PLOT_ZONE).toLocalDate().minusDays(1);
        final SortedMap<LocalDate, Double> maxima;
        try {
            maxima = externalPhenologyService.findDailyMaxTemperatures(plotId, lastDay.minusDays(TREND_DAYS - 1), lastDay);
        } catch (RuntimeException exception) {
            // Without the observed weather the alert falls back to the node series, like the other types
            return List.of();
        }
        return maxima.entrySet().stream()
                .map(day -> new WeeklyTrendPoint(day.getKey().atStartOfDay(PLOT_ZONE).toInstant(), day.getValue(), threshold))
                .toList();
    }

    /**
     * Keeps, for each 24 h window ending at the trigger, the reading that matters for the incident:
     * the hottest hour for a heat wave or a warm winter, the coldest for a frost and the driest for hydric stress.
     * The virtual node stores one reading per hour, and the app draws one point per day.
     */
    private List<WeeklyTrendPoint> dailyExtremes(
            List<HourlyTelemetryReadingSnapshot> readings,
            IncidentType type,
            Instant start,
            Double threshold
    ) {
        Comparator<HourlyTelemetryReadingSnapshot> byValue = Comparator.comparing(reading -> extractMetricValue(reading, type));
        Comparator<HourlyTelemetryReadingSnapshot> worstFirst = type == IncidentType.HEAT_WAVE || type == IncidentType.WARM_WINTER
                ? byValue.reversed() : byValue;

        var byDay = new TreeMap<Long, HourlyTelemetryReadingSnapshot>();
        for (HourlyTelemetryReadingSnapshot reading : readings) {
            var timestamp = reading.timestamp().timestamp();
            if (!timestamp.isAfter(start)) {
                continue;
            }
            // Windows are (start + d days, start + d + 1 days], so a reading exactly at the trigger counts
            long day = (Duration.between(start, timestamp).toMillis() - 1) / Duration.ofDays(1).toMillis();
            byDay.merge(day, reading, (current, candidate) -> worstFirst.compare(candidate, current) < 0 ? candidate : current);
        }
        return byDay.values().stream()
                .map(reading -> new WeeklyTrendPoint(reading.timestamp().timestamp(), extractMetricValue(reading, type), threshold))
                .toList();
    }

    private Double extractMetricValue(HourlyTelemetryReadingSnapshot reading, IncidentType type) {
        if (type == IncidentType.HYDRIC_STRESS) {
            return reading.soilMoisture() != null ? reading.soilMoisture().percentage() : 0.0;
        } else {
            return reading.temperature() != null ? reading.temperature().celsius() : 0.0;
        }
    }
}
