package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices.AgroclimaticIncidentQueryServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.AgroclimaticIncidentDetail;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.AgroclimaticIncidentsSummary;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgroclimaticIncidentQueryService Application Unit Tests")
class AgroclimaticIncidentQueryServiceTest {

    @Mock
    private AgroclimaticIncidentRepository incidentRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private TelemetrySeriesRepository telemetrySeriesRepository;

    private AgroclimaticIncidentQueryServiceImpl queryService;

    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    @BeforeEach
    void setUp() {
        queryService = new AgroclimaticIncidentQueryServiceImpl(incidentRepository, externalOrchardService, telemetrySeriesRepository);
    }

    @Test
    @DisplayName("Should retrieve summarized incidents successfully")
    void shouldRetrieveSummarizedIncidentsSuccessfully() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(externalOrchardService.findPlotName(plotId)).thenReturn(Optional.of("Lote Norte"));
        when(externalOrchardService.findPlotVariety(plotId)).thenReturn(Optional.of("ARBEQUINA"));

        var incident = AgroclimaticIncident.raise(
                plotId,
                IncidentType.HEAT_WAVE,
                IncidentSeverity.CRITICAL,
                new ThresholdBreachInfo("AMBIENT_TEMPERATURE", 36.5, 36.0, "°C"),
                List.of("heat_wave.step.irrigate_early_morning"),
                Instant.now()
        );

        when(incidentRepository.findAll(plotId, IncidentStatus.ACTIVE, null)).thenReturn(List.of(incident));
        when(incidentRepository.findAll(plotId, null, null)).thenReturn(List.of(incident));

        var query = new GetAgroclimaticIncidentsQuery(plotId.plotId(), "ACTIVE", null);
        var result = queryService.handle(query);

        assertThat(result).isInstanceOf(Result.Success.class);
        var summary = ((Result.Success<AgroclimaticIncidentsSummary, ?>) result).value();
        assertThat(summary.activeCount()).isEqualTo(1L);
        assertThat(summary.criticalCount()).isEqualTo(1L);
        assertThat(summary.warningCount()).isZero();
        assertThat(summary.incidents()).hasSize(1);
        assertThat(summary.incidents().get(0).plotName()).isEqualTo("Lote Norte");
        assertThat(summary.incidents().get(0).plotVariety()).isEqualTo("ARBEQUINA");
    }

    @Test
    @DisplayName("Should retrieve incident detail with weekly trend successfully")
    void shouldRetrieveIncidentDetailSuccessfully() {
        when(externalOrchardService.findPlotName(plotId)).thenReturn(Optional.of("Lote Sur"));
        when(externalOrchardService.findPlotVariety(plotId)).thenReturn(Optional.of("CRIOLLA"));

        var incident = AgroclimaticIncident.raise(
                plotId,
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.WARNING,
                new ThresholdBreachInfo("SOIL_MOISTURE_30CM", 18.0, 20.0, "%"),
                List.of("hydric_stress.step.check_drippers"),
                Instant.now()
        );
        var incidentId = incident.snapshot().id();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(any(), any(), any())).thenReturn(List.of());

        var query = new GetAgroclimaticIncidentByIdQuery(incidentId.incidentId());
        var result = queryService.handle(query);

        assertThat(result).isInstanceOf(Result.Success.class);
        var detail = ((Result.Success<AgroclimaticIncidentDetail, ?>) result).value();
        assertThat(detail.plotName()).isEqualTo("Lote Sur");
        assertThat(detail.plotVariety()).isEqualTo("CRIOLLA");
        assertThat(detail.weeklyTrend()).isNotEmpty();
    }
}
