package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.WeatherForecastQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.WeatherForecastIngestedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application query service implementation orchestrating weather forecast queries.
 *
 * <p>Validates plot existence and active status in Orchard via ACL, retrieves plot centroid coordinates,
 * queries the weather forecast provider, and publishes {@link WeatherForecastIngestedEvent}.</p>
 */
@Service
@Transactional(readOnly = true)
public class WeatherForecastQueryServiceImpl implements WeatherForecastQueryService {

    private final WeatherForecastProvider weatherForecastProvider;
    private final ExternalOrchardService externalOrchardService;
    private final ApplicationEventPublisher eventPublisher;

    public WeatherForecastQueryServiceImpl(
            WeatherForecastProvider weatherForecastProvider,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            ApplicationEventPublisher eventPublisher
    ) {
        if (weatherForecastProvider == null) {
            throw new IllegalArgumentException("telemetry.weather_provider.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("telemetry.event_publisher.null");
        }
        this.weatherForecastProvider = weatherForecastProvider;
        this.externalOrchardService = externalOrchardService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<WeatherForecastSnapshot, ApplicationError> handle(GetWeatherForecastByPlotIdQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query.null");
        }

        var plotId = query.plotId();
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }

        var centroidOpt = externalOrchardService.findPlotCentroid(plotId);
        double latitude = centroidOpt.map(c -> c[0]).orElse(-18.05);
        double longitude = centroidOpt.map(c -> c[1]).orElse(-70.25);

        var forecastDays = weatherForecastProvider.fetchSevenDayForecast(latitude, longitude);
        var daySnapshots = forecastDays.stream()
                .map(WeatherForecastDay::snapshot)
                .toList();

        var snapshot = new WeatherForecastSnapshot(plotId, daySnapshots, Instant.now());

        if (!forecastDays.isEmpty()) {
            var firstDay = forecastDays.get(0);
            double lowestTemp = forecastDays.stream()
                    .mapToDouble(d -> d.minTemperature().celsius())
                    .min()
                    .orElse(firstDay.minTemperature().celsius());

            eventPublisher.publishEvent(new WeatherForecastIngestedEvent(
                    plotId.plotId(),
                    firstDay.forecastDate(),
                    lowestTemp,
                    Instant.now()
            ));
        }

        return Result.success(snapshot);
    }
}
