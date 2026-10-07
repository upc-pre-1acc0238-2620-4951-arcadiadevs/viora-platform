package com.arcadiadevs.viora.platform.telemetry.infrastructure.adapters.meteo;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * External infrastructure adapter implementing {@link HourlyWeatherProvider} with the Open-Meteo REST API.
 *
 * <p>Asks for the hourly air temperature, relative humidity, soil moisture of the 27–81 cm layer (the one
 * that holds the 30 cm probe) and shortwave radiation of the past days, in UTC. Hours that have not
 * started yet are dropped. When Open-Meteo cannot be reached it returns an empty list, so no invented
 * reading is ever stored.</p>
 */
@Component
public class OpenMeteoHourlyWeatherAdapter implements HourlyWeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoHourlyWeatherAdapter.class);
    private static final String OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String HOURLY_VARIABLES =
            "temperature_2m,relative_humidity_2m,soil_moisture_27_to_81cm,shortwave_radiation";

    private final RestClient restClient;

    public OpenMeteoHourlyWeatherAdapter() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(4).toMillis());
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(OPEN_METEO_BASE_URL)
                .build();
    }

    public OpenMeteoHourlyWeatherAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<HourlyWeatherObservation> fetchPastHours(double latitude, double longitude, int pastDays) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("hourly", HOURLY_VARIABLES)
                            .queryParam("timezone", "UTC")
                            .queryParam("past_days", pastDays)
                            .queryParam("forecast_days", 1)
                            .build())
                    .retrieve()
                    .body(OpenMeteoHourlyResponse.class);

            if (response == null || response.hourly() == null || response.hourly().time() == null) {
                return List.of();
            }
            return parse(response.hourly(), Instant.now());
        } catch (Exception ex) {
            log.warn("Open-Meteo hourly weather unreachable ({}) for ({}, {}). The virtual node keeps its current readings.",
                    ex.getMessage(), latitude, longitude);
            return List.of();
        }
    }

    static List<HourlyWeatherObservation> parse(HourlyData hourly, Instant now) {
        var observations = new ArrayList<HourlyWeatherObservation>();
        for (int i = 0; i < hourly.time().size(); i++) {
            var hour = LocalDateTime.parse(hourly.time().get(i)).toInstant(ZoneOffset.UTC);
            if (hour.isAfter(now)) {
                break;
            }
            var temperature = valueAt(hourly.temperature(), i);
            var humidity = valueAt(hourly.relativeHumidity(), i);
            var soilMoisture = valueAt(hourly.soilMoisture(), i);
            var radiation = valueAt(hourly.shortwaveRadiation(), i);
            if (temperature == null || humidity == null || soilMoisture == null || radiation == null) {
                continue;
            }
            observations.add(new HourlyWeatherObservation(
                    hour,
                    temperature,
                    humidity,
                    soilMoisture * 100.0,
                    radiation
            ));
        }
        return observations;
    }

    private static Double valueAt(List<Double> values, int index) {
        return values != null && index < values.size() ? values.get(index) : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenMeteoHourlyResponse(HourlyData hourly) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HourlyData(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature,
            @JsonProperty("relative_humidity_2m") List<Double> relativeHumidity,
            @JsonProperty("soil_moisture_27_to_81cm") List<Double> soilMoisture,
            @JsonProperty("shortwave_radiation") List<Double> shortwaveRadiation
    ) {
    }
}
