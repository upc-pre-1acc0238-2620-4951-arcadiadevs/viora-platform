package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices.WeatherForecastQueryServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.WeatherForecastIngestedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.AmbientTemperature;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.Percentage;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.WindSpeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherForecastQueryService Application Unit Tests")
class WeatherForecastQueryServiceTest {

    @Mock
    private WeatherForecastProvider weatherForecastProvider;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WeatherForecastQueryServiceImpl service;

    private final PlotId plotId = new PlotId();

    @BeforeEach
    void setUp() {
        service = new WeatherForecastQueryServiceImpl(
                weatherForecastProvider,
                externalOrchardService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("Should successfully retrieve 7-day forecast and publish domain event when plot is active")
    void shouldSuccessfullyRetrieveForecastAndPublishEvent() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(externalOrchardService.findPlotCentroid(plotId)).thenReturn(Optional.of(new double[]{-18.05, -70.25}));

        var day1 = WeatherForecastDay.create(
                LocalDate.now(),
                new AmbientTemperature(25.0),
                new AmbientTemperature(1.0),
                new Percentage(10.0),
                new WindSpeed(15.0),
                Instant.now()
        );
        when(weatherForecastProvider.fetchSevenDayForecast(-18.05, -70.25)).thenReturn(List.of(day1));

        var query = new GetWeatherForecastByPlotIdQuery(plotId);
        var result = service.handle(query);

        assertThat(result.isSuccess()).isTrue();
        var snapshot = result.success().orElseThrow();
        assertThat(snapshot.plotId()).isEqualTo(plotId);
        assertThat(snapshot.dailyForecasts()).hasSize(1);
        assertThat(snapshot.dailyForecasts().get(0).isFrostRisk()).isTrue();

        var eventCaptor = ArgumentCaptor.forClass(WeatherForecastIngestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().plotId()).isEqualTo(plotId.plotId());
        assertThat(eventCaptor.getValue().minTemp()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should return notFound application error when plot does not exist or is inactive")
    void shouldReturnNotFoundWhenPlotIsInactive() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(false);

        var query = new GetWeatherForecastByPlotIdQuery(plotId);
        var result = service.handle(query);

        assertThat(result.isFailure()).isTrue();
        var error = result.failure().orElseThrow();
        assertThat(error.code()).isEqualTo("PLOT_NOT_FOUND");
        assertThat(error.message()).contains(plotId.plotId());

        verify(externalOrchardService).existsActivePlot(plotId);
        verifyNoInteractions(weatherForecastProvider);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should validate constructor and method preconditions")
    void shouldValidatePreconditions() {
        assertThatThrownBy(() -> new WeatherForecastQueryServiceImpl(null, externalOrchardService, eventPublisher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.weather_provider.null");

        assertThatThrownBy(() -> new WeatherForecastQueryServiceImpl(weatherForecastProvider, null, eventPublisher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device.external_orchard_service.null");

        assertThatThrownBy(() -> new WeatherForecastQueryServiceImpl(weatherForecastProvider, externalOrchardService, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.event_publisher.null");

        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query.null");
    }
}
