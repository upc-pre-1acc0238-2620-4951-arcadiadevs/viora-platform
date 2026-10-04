package com.arcadiadevs.viora.platform.telemetry.infrastructure.seeding;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReading;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Demo and production baseline data seeder for Agroclimatic Incidents and Telemetry Series.
 *
 * <p>Ensures that the default demo producer has active and normalized alerts
 * matching the Figma mockups (P10 Home widget and T14/T15 Alert Center),
 * with interactive checklist progress and a 7-day weekly trend curve.</p>
 */
@Component
@Order(2)
public class AgroclimaticIncidentDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AgroclimaticIncidentDataSeeder.class);

    private final AgroclimaticIncidentRepository incidentRepository;
    private final TelemetrySeriesRepository telemetrySeriesRepository;
    private final ExternalOrchardService externalOrchardService;
    private final String defaultProducerId;

    public AgroclimaticIncidentDataSeeder(
            AgroclimaticIncidentRepository incidentRepository,
            TelemetrySeriesRepository telemetrySeriesRepository,
            ExternalOrchardService externalOrchardService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultProducerId
    ) {
        this.incidentRepository = incidentRepository;
        this.telemetrySeriesRepository = telemetrySeriesRepository;
        this.externalOrchardService = externalOrchardService;
        this.defaultProducerId = defaultProducerId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // 1. Fetch active plots for the default test producer via ACL
            var activePlotIds = externalOrchardService.findActivePlotIdsByProducerId(defaultProducerId);
            if (activePlotIds.isEmpty()) {
                log.warn("No active plots found for default producer {}. Cannot bind agroclimatic incidents.", defaultProducerId);
                return;
            }

            var plotA = activePlotIds.get(0);
            var plotB = (activePlotIds.size() > 1) ? activePlotIds.get(1) : plotA;

            // 2. Idempotency: verify if incidents already exist for plotA
            var existingIncidents = incidentRepository.findByPlotId(plotA);
            if (!existingIncidents.isEmpty()) {
                log.info("Agroclimatic incidents already exist for test plot {}. Skipping demo data seeding.", plotA.plotId());
                return;
            }

            var now = Instant.now();

            // 3. Incident 1: Heat Wave (Critical, Active) with Step 1 completed (Figma T15: 1 of 3)
            var heatWaveIncident = AgroclimaticIncident.raise(
                    plotA,
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.CRITICAL,
                    new ThresholdBreachInfo("temperatura_maxima", 34.0, 32.0, "°C"),
                    List.of(
                            "heat_wave.step.pre_irrigation",
                            "heat_wave.step.morning_irrigation",
                            "heat_wave.step.suspend_cultural_ops"
                    ),
                    now
            );

            if (!heatWaveIncident.snapshot().mitigationSteps().isEmpty()) {
                var firstStepId = heatWaveIncident.snapshot().mitigationSteps().get(0).id();
                heatWaveIncident.completeMitigationStep(firstStepId, now.minus(Duration.ofHours(14)));
            }
            incidentRepository.save(heatWaveIncident);

            // 4. Incident 2: Hydric Stress (Warning, Active) with 3 pending steps
            var hydricStressIncident = AgroclimaticIncident.raise(
                    plotB,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.WARNING,
                    new ThresholdBreachInfo("humedad_suelo_30cm", 16.0, 20.0, "%"),
                    List.of(
                            "hydric_stress.step.check_drippers",
                            "hydric_stress.step.schedule_emergency_irrigation",
                            "hydric_stress.step.measure_stem_potential"
                    ),
                    now.minus(Duration.ofHours(3))
            );
            incidentRepository.save(hydricStressIncident);

            // 5. Incident 3: Hydric Stress (Normalized in past 7 days)
            var normalizedIncident1 = AgroclimaticIncident.raise(
                    plotA,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.WARNING,
                    new ThresholdBreachInfo("humedad_suelo_30cm", 22.0, 20.0, "%"),
                    List.of(),
                    now.minus(Duration.ofDays(2))
            );
            normalizedIncident1.normalize(now.minus(Duration.ofDays(1)));
            incidentRepository.save(normalizedIncident1);

            // 6. Incident 4: Frost Warning (Normalized in past 7 days)
            var normalizedIncident2 = AgroclimaticIncident.raise(
                    plotB,
                    IncidentType.FROST_WARNING,
                    IncidentSeverity.WARNING,
                    new ThresholdBreachInfo("temperatura_minima", 4.5, 3.0, "°C"),
                    List.of(),
                    now.minus(Duration.ofDays(5))
            );
            normalizedIncident2.normalize(now.minus(Duration.ofDays(4)));
            incidentRepository.save(normalizedIncident2);

            // 7. Seed Weekly Trend telemetry readings for Plot A (Figma T15 7-day curve)
            var series = telemetrySeriesRepository.findByPlotId(plotA)
                    .orElseGet(() -> TelemetrySeries.create(plotA, null));

            if (series.readings().isEmpty()) {
                double[] dailyPeakTemperatures = {29.0, 30.0, 34.0, 31.0, 28.0, 27.0, 34.0};
                for (int i = 0; i < dailyPeakTemperatures.length; i++) {
                    int daysAgo = (dailyPeakTemperatures.length - 1) - i;
                    var readingTime = now.minus(Duration.ofHours(daysAgo * 24L)).plus(Duration.ofMinutes(5));
                    var reading = HourlyTelemetryReading.create(
                            new ReadingTimestamp(readingTime),
                            new AmbientTemperature(dailyPeakTemperatures[i]),
                            new RelativeHumidity(45.0),
                            new SoilMoisture(22.0),
                            new SolarRadiation(750.0),
                            null
                    );
                    series.ingestReading(reading);
                }
                telemetrySeriesRepository.save(series);
            }

            log.info("AgroclimaticIncidentDataSeeder successfully initialized 4 incidents (2 active, 2 normalized) and weekly telemetry series for plots: {} and {}.",
                    plotA.plotId(), plotB.plotId());

        } catch (Exception ex) {
            log.error("Unexpected error during AgroclimaticIncidentDataSeeder execution: {}", ex.getMessage(), ex);
        }
    }
}
