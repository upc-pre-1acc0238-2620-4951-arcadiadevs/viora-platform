package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices.TelemetryQueryServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryQueryService Application Unit Tests")
class TelemetryQueryServiceTest {

    @Mock
    private TelemetrySeriesRepository telemetrySeriesRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    private TelemetryQueryServiceImpl telemetryQueryService;

    private final PlotId plotId = new PlotId();

    @BeforeEach
    void setUp() {
        telemetryQueryService = new TelemetryQueryServiceImpl(
                telemetrySeriesRepository,
                externalOrchardService
        );
    }

    @Test
    @DisplayName("Should return failure with notFound when plot does not exist or is inactive in Orchard")
    void shouldReturnNotFoundWhenPlotIsInactiveOrDoesNotExist() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(false);

        var query = new GetTelemetrySeriesByPlotIdQuery(plotId, null, null);
        var result = telemetryQueryService.handle(query);

        assertThat(result.isFailure()).isTrue();
        var error = result.failure().orElseThrow();
        assertThat(error.code()).isEqualTo("PLOT_NOT_FOUND");
        assertThat(error.message()).contains(plotId.plotId());

        verify(externalOrchardService).existsActivePlot(plotId);
        verifyNoInteractions(telemetrySeriesRepository);
    }

    @Test
    @DisplayName("Should return readings list when plot exists and is active")
    void shouldReturnReadingsWhenPlotIsActive() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);

        var reading = new HourlyTelemetryReadingSnapshot(
                new HourlyReadingId(),
                new ReadingTimestamp(Instant.now()),
                new AmbientTemperature(24.0),
                new RelativeHumidity(60.0),
                new SoilMoisture(28.0),
                new SolarRadiation(700.0),
                null
        );

        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(plotId, null, null))
                .thenReturn(List.of(reading));

        var query = new GetTelemetrySeriesByPlotIdQuery(plotId, null, null);
        var result = telemetryQueryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        var readings = result.success().orElseThrow();
        assertThat(readings).hasSize(1);
        assertThat(readings.get(0).temperature().celsius()).isEqualTo(24.0);

        verify(externalOrchardService).existsActivePlot(plotId);
        verify(telemetrySeriesRepository).findReadingsByPlotIdAndDateRange(plotId, null, null);
    }

    @Test
    @DisplayName("Should pass date range filters accurately to the repository port")
    void shouldFilterByDateRange() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);

        var start = Instant.parse("2026-09-01T00:00:00Z");
        var end = Instant.parse("2026-09-02T00:00:00Z");

        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(plotId, start, end))
                .thenReturn(List.of());

        var query = new GetTelemetrySeriesByPlotIdQuery(plotId, start, end);
        var result = telemetryQueryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow()).isEmpty();

        verify(telemetrySeriesRepository).findReadingsByPlotIdAndDateRange(plotId, start, end);
    }
}
