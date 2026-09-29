package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDaySnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastSnapshot;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.DailyForecastDto;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.WeatherForecastResource;

import java.util.List;

/**
 * Assembler converting domain {@link WeatherForecastSnapshot} to public {@link WeatherForecastResource}.
 */
public final class WeatherForecastResourceAssembler {

    private WeatherForecastResourceAssembler() {}

    /**
     * Transforms an aggregated domain forecast snapshot to a REST presentation resource.
     *
     * @param snapshot the domain weather forecast snapshot
     * @return the populated REST resource
     */
    public static WeatherForecastResource toResourceFromEntity(WeatherForecastSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        List<DailyForecastDto> dtos = snapshot.dailyForecasts().stream()
                .map(WeatherForecastResourceAssembler::toDailyDto)
                .toList();

        return new WeatherForecastResource(
                snapshot.plotId().plotId(),
                dtos,
                snapshot.generatedAt()
        );
    }

    private static DailyForecastDto toDailyDto(WeatherForecastDaySnapshot day) {
        return new DailyForecastDto(
                day.forecastDate(),
                day.maxTemperature().celsius(),
                day.minTemperature().celsius(),
                day.precipitationProbability().value(),
                day.windSpeedKmh().kmh(),
                day.isFrostRisk(),
                day.syncedAt()
        );
    }
}
