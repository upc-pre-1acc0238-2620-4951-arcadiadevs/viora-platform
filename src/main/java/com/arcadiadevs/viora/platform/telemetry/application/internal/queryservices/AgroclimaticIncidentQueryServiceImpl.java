package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.*;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link AgroclimaticIncidentQueryService} orchestrating read queries for incidents.
 */
@Service
@Transactional(readOnly = true)
public class AgroclimaticIncidentQueryServiceImpl implements AgroclimaticIncidentQueryService {

    private final AgroclimaticIncidentRepository incidentRepository;
    private final ExternalOrchardService externalOrchardService;
    private final TelemetrySeriesRepository telemetrySeriesRepository;

    /**
     * Constructs the AgroclimaticIncidentQueryServiceImpl with required dependencies.
     *
     * @param incidentRepository        the incident domain repository
     * @param externalOrchardService    the outbound ACL service for orchard lookups
     * @param telemetrySeriesRepository the telemetry readings repository
     */
    public AgroclimaticIncidentQueryServiceImpl(
            AgroclimaticIncidentRepository incidentRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            TelemetrySeriesRepository telemetrySeriesRepository
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

        // Compute weekly trend
        Instant start = snapshot.triggeredAt().minus(Duration.ofDays(6));
        Instant end = snapshot.triggeredAt().plus(Duration.ofDays(1));
        List<HourlyTelemetryReadingSnapshot> readings = telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(plotId, start, end);

        List<WeeklyTrendPoint> trendPoints = new ArrayList<>();
        Double threshold = snapshot.breachInfo().thresholdValue();

        if (readings != null && !readings.isEmpty()) {
            for (HourlyTelemetryReadingSnapshot reading : readings) {
                Double value = extractMetricValue(reading, snapshot.type());
                trendPoints.add(new WeeklyTrendPoint(reading.timestamp().timestamp(), value, threshold));
            }
        } else {
            // Synthesize single observation point at trigger time
            trendPoints.add(new WeeklyTrendPoint(snapshot.triggeredAt(), snapshot.breachInfo().currentValue(), threshold));
        }

        return Result.success(new AgroclimaticIncidentDetail(snapshot, plotName, plotVariety, trendPoints));
    }

    private Double extractMetricValue(HourlyTelemetryReadingSnapshot reading, IncidentType type) {
        if (type == IncidentType.HYDRIC_STRESS) {
            return reading.soilMoisture() != null ? reading.soilMoisture().percentage() : 0.0;
        } else {
            return reading.temperature() != null ? reading.temperature().celsius() : 0.0;
        }
    }
}
