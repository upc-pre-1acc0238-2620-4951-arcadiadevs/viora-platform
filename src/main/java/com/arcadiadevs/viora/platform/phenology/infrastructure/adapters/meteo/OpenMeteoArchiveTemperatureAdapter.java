package com.arcadiadevs.viora.platform.phenology.infrastructure.adapters.meteo;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * External infrastructure adapter implementing {@link HourlyTemperatureProvider} with the Open-Meteo
 * Historical Weather API (reanalysis at the plot coordinates, available up to the previous day).
 *
 * <p>Days already closed never change, so a range that ends two days ago or earlier is cached for good; a range
 * that reaches the last days is cached for one hour. When Open-Meteo cannot be reached it returns an empty
 * optional, so no temperature is ever invented.</p>
 */
@Component
public class OpenMeteoArchiveTemperatureAdapter implements HourlyTemperatureProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoArchiveTemperatureAdapter.class);
    private static final String OPEN_METEO_ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final Duration RECENT_RANGE_TTL = Duration.ofHours(1);
    private static final int MAX_CACHED_RANGES = 500;

    private final RestClient restClient;
    private final Clock clock;
    private final Map<String, CachedRange> cache = new ConcurrentHashMap<>();

    @Autowired
    public OpenMeteoArchiveTemperatureAdapter(Clock clock) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(8).toMillis());
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(OPEN_METEO_ARCHIVE_URL)
                .build();
        this.clock = clock;
    }

    public OpenMeteoArchiveTemperatureAdapter(RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    @Override
    public Optional<List<HourlyTemperature>> fetchHourlyTemperatures(double latitude, double longitude, LocalDate from, LocalDate to) {
        var key = String.format(Locale.ROOT, "%.3f,%.3f,%s,%s", latitude, longitude, from, to);
        var now = clock.instant();
        var cached = cache.get(key);
        if (cached != null && (cached.expiresAt() == null || now.isBefore(cached.expiresAt()))) {
            return Optional.of(cached.hours());
        }

        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("start_date", from)
                            .queryParam("end_date", to)
                            .queryParam("hourly", "temperature_2m")
                            .queryParam("timezone", PLOT_TIME_ZONE.getId())
                            .build())
                    .retrieve()
                    .body(OpenMeteoArchiveResponse.class);

            if (response == null || response.hourly() == null || response.hourly().time() == null) {
                return Optional.empty();
            }
            var hours = parse(response.hourly());
            remember(key, hours, to, now);
            return Optional.of(hours);
        } catch (Exception ex) {
            log.warn("Open-Meteo archive unreachable ({}) for ({}, {}) {}..{}. The chill metric is not evaluated.",
                    ex.getMessage(), latitude, longitude, from, to);
            return Optional.empty();
        }
    }

    private void remember(String key, List<HourlyTemperature> hours, LocalDate to, Instant now) {
        if (cache.size() >= MAX_CACHED_RANGES) {
            cache.clear();
        }
        var today = LocalDate.ofInstant(now, PLOT_TIME_ZONE);
        var closed = to.isBefore(today.minusDays(1));
        cache.put(key, new CachedRange(List.copyOf(hours), closed ? null : now.plus(RECENT_RANGE_TTL)));
    }

    static List<HourlyTemperature> parse(HourlyData hourly) {
        var hours = new ArrayList<HourlyTemperature>();
        for (int i = 0; i < hourly.time().size(); i++) {
            var temperature = hourly.temperature() != null && i < hourly.temperature().size()
                    ? hourly.temperature().get(i)
                    : null;
            if (temperature == null) {
                continue;
            }
            hours.add(new HourlyTemperature(LocalDateTime.parse(hourly.time().get(i)), temperature));
        }
        return hours;
    }

    private record CachedRange(List<HourlyTemperature> hours, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenMeteoArchiveResponse(HourlyData hourly) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HourlyData(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature
    ) {
    }
}
