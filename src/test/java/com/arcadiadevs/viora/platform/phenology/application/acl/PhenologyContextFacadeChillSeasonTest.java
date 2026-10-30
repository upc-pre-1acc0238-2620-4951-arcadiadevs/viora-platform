package com.arcadiadevs.viora.platform.phenology.application.acl;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotChillSeason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PhenologyContextFacadeImpl chill season of a plot")
class PhenologyContextFacadeChillSeasonTest {

    /** 2026-07-01 at 10:00 in Lima: the winter of 2026 is in progress, June is over. */
    private static final Instant IN_SEASON = Instant.parse("2026-07-01T15:00:00Z");

    @Mock
    private HarvestRecordQueryService harvestRecordQueryService;
    @Mock
    private ExternalOrchardService externalOrchardService;
    @Mock
    private HourlyTemperatureProvider temperatureProvider;

    private final MutableClock clock = new MutableClock(IN_SEASON);
    private PhenologyContextFacadeImpl facade;
    private final String plotId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        facade = new PhenologyContextFacadeImpl(harvestRecordQueryService, externalOrchardService,
                temperatureProvider, clock);
        when(externalOrchardService.findPlotCentroid(any(PlotId.class)))
                .thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class),
                any(LocalDate.class))).thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    LocalDate to = invocation.getArgument(3);
                    double celsius = from.getYear() == 2026 ? 6.0 : 15.0;
                    return Optional.of(days(from, (int) (to.toEpochDay() - from.toEpochDay()) + 1, celsius));
                });
    }

    @Test
    @DisplayName("The winter in progress, evaluated with the varietal requirement up to yesterday")
    void winterInProgress() {
        assertThat(facade.currentChillSeasonYear()).isEqualTo(2026);

        var season = facade.findChillSeason(plotId, 2026).orElseThrow();

        assertThat(season.seasonYear()).isEqualTo(2026);
        assertThat(season.accumulatedPortions()).isCloseTo(24.43, offset(0.01));
        assertThat(season.thresholdPortions()).isEqualTo(30.0);
        assertThat(season.seasonState()).isEqualTo("IN_PROGRESS");
        assertThat(season.projectionStatus()).isEqualTo("PROJECTED");
        assertThat(season.projectedCompletionDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(season.completionDate()).isNull();
        assertThat(season.seasonEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(season.evaluatedThrough()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(season.daysAbove24Celsius()).isZero();
        assertThat(season.warmDayCelsius()).isEqualTo(24.0);
        assertThat(season.dailyPortions()).hasSize(30);
        assertThat(season.dailyPortions().get(LocalDate.of(2026, 6, 30))).isCloseTo(24.43, offset(0.01));
        verify(temperatureProvider).fetchHourlyTemperatures(-18.0, -70.25, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30));
    }

    @Test
    @DisplayName("Last winter is the previous season, evaluated over its whole season")
    void lastWinter() {
        var season = facade.findChillSeason(plotId, 2025).orElseThrow();

        assertThat(season.seasonYear()).isEqualTo(2025);
        assertThat(season.seasonState()).isEqualTo("OFF_SEASON");
        assertThat(season.projectionStatus()).isEqualTo("NOT_APPLICABLE");
        assertThat(season.evaluatedThrough()).isEqualTo(LocalDate.of(2025, 8, 31));
        assertThat(season.accumulatedPortions()).isZero();
        assertThat(season.dailyPortions()).hasSize(92);
        verify(temperatureProvider).fetchHourlyTemperatures(-18.0, -70.25, LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 31));
    }

    @Test
    @DisplayName("A season is read once within the TTL and again once it expires")
    void cachedWithinTtl() {
        facade.findChillSeason(plotId, 2026);
        clock.advance(PhenologyContextFacadeImpl.CHILL_SEASON_TTL.minusMinutes(1));
        facade.findChillSeason(plotId, 2026);
        verify(temperatureProvider, times(1)).fetchHourlyTemperatures(anyDouble(), anyDouble(),
                any(LocalDate.class), any(LocalDate.class));

        clock.advance(Duration.ofMinutes(2));
        assertThat(facade.findChillSeason(plotId, 2026)).isPresent();
        verify(temperatureProvider, times(2)).fetchHourlyTemperatures(anyDouble(), anyDouble(),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("A finished season is kept longer than one in progress")
    void finishedSeasonKeptLonger() {
        facade.findChillSeason(plotId, 2025);
        clock.advance(PhenologyContextFacadeImpl.CHILL_SEASON_TTL.plusMinutes(1));
        facade.findChillSeason(plotId, 2025);

        verify(temperatureProvider, times(1)).fetchHourlyTemperatures(anyDouble(), anyDouble(),
                any(LocalDate.class), any(LocalDate.class));
        assertThat(PhenologyContextFacadeImpl.FINISHED_SEASON_TTL).isGreaterThan(
                PhenologyContextFacadeImpl.CHILL_SEASON_TTL);
    }

    @Test
    @DisplayName("Weather unavailable or failing gives nothing, and the failure is never cached")
    void failureNotCached() {
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class),
                any(LocalDate.class))).thenReturn(Optional.empty())
                .thenThrow(new IllegalStateException("open-meteo down"))
                .thenReturn(Optional.of(days(LocalDate.of(2026, 6, 1), 30, 6.0)));

        assertThat(facade.findChillSeason(plotId, 2026)).isEmpty();
        assertThat(facade.findChillSeason(plotId, 2026)).isEmpty();
        assertThat(facade.findChillSeason(plotId, 2026)).isPresent();
        verify(temperatureProvider, times(3)).fetchHourlyTemperatures(anyDouble(), anyDouble(),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Unknown plot, plot without location, malformed id or future season: nothing, no weather call")
    void nothingToRead() {
        assertThat(facade.findChillSeason(plotId, 2027)).isEmpty();
        assertThat(facade.findChillSeason("not-a-uuid", 2026)).isEmpty();
        assertThat(facade.findChillSeason(null, 2026)).isEmpty();
        when(externalOrchardService.findPlotCentroid(any(PlotId.class))).thenReturn(Optional.empty());
        assertThat(facade.findChillSeason(plotId, 2026)).isEmpty();
        when(externalOrchardService.findPlotCentroid(any(PlotId.class)))
                .thenThrow(new IllegalStateException("orchard down"));
        assertThat(facade.findChillSeason(plotId, 2026)).isEmpty();

        verify(temperatureProvider, never()).fetchHourlyTemperatures(anyDouble(), anyDouble(),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Concurrent misses of the same season fetch the weather once")
    void concurrentMissesFetchOnce() throws Exception {
        var calls = new AtomicInteger();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(temperatureProvider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(LocalDate.class),
                any(LocalDate.class))).thenAnswer(invocation -> {
                    calls.incrementAndGet();
                    started.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return Optional.of(days(LocalDate.of(2026, 6, 1), 30, 6.0));
                });
        var executor = Executors.newFixedThreadPool(4);
        try {
            var results = new ArrayList<java.util.concurrent.Future<Optional<PlotChillSeason>>>();
            for (int i = 0; i < 4; i++) {
                results.add(executor.submit(() -> facade.findChillSeason(plotId, 2026)));
            }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);
            release.countDown();
            for (var result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isPresent();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(calls).hasValue(1);
    }

    private static List<HourlyTemperature> days(LocalDate from, int count, double celsius) {
        var hours = new ArrayList<HourlyTemperature>();
        for (int day = 0; day < count; day++) {
            for (int hour = 0; hour < 24; hour++) {
                hours.add(new HourlyTemperature(from.plusDays(day).atTime(hour, 0), celsius));
            }
        }
        return hours;
    }

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {

        private volatile Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
