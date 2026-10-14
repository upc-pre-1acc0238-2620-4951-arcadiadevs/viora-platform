package com.arcadiadevs.viora.platform.phenology.application;

import com.arcadiadevs.viora.platform.phenology.application.internal.commandservices.WinterWarmthMonitor;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.domain.model.events.WinterWarmthEvaluatedEvent;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Winter warmth monitor (US23)")
class WinterWarmthMonitorTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final HourlyTemperatureProvider provider = mock(HourlyTemperatureProvider.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final String plotId = UUID.randomUUID().toString();

    private WinterWarmthMonitor monitorOn(LocalDate today) {
        var clock = Clock.fixed(today.atTime(9, 0).atZone(LIMA).toInstant(), LIMA);
        return new WinterWarmthMonitor(orchard, provider, publisher, clock);
    }

    /** Cold nights at 6 °C; afternoons at 14 °C, except the last {@code warmDays} days that peak at {@code warmMax}. */
    private static List<HourlyTemperature> winter(LocalDate from, LocalDate to, int warmDays, double warmMax) {
        var hours = new ArrayList<HourlyTemperature>();
        for (var day = from; !day.isAfter(to); day = day.plusDays(1)) {
            boolean warm = day.isAfter(to.minusDays(warmDays));
            for (int hour = 0; hour < 24; hour++) {
                double celsius = hour >= 11 && hour <= 16 ? (warm ? warmMax : 14.0) : 6.0;
                hours.add(new HourlyTemperature(day.atTime(hour, 0), celsius));
            }
        }
        return hours;
    }

    @Test
    @DisplayName("Five days above 24 °C in July make an active warm spell")
    void publishesAnActiveSpell() {
        var today = LocalDate.of(2026, 7, 18);
        when(orchard.findPlotCentroid(any())).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(provider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any()))
                .thenReturn(Optional.of(winter(LocalDate.of(2026, 6, 1), today.minusDays(1), 5, 27.0)));

        var event = monitorOn(today).checkPlot(new PlotId(plotId), today).orElseThrow();

        assertTrue(event.isWarmSpellActive());
        assertEquals(5, event.currentWarmStreakDays());
        assertEquals(27.0, event.latestMaxTemperatureCelsius());
        assertEquals(24.0, event.warmDayThresholdCelsius());
        assertEquals(today.minusDays(1), event.evaluatedThrough());
        assertEquals(2026, event.seasonYear());
        verify(publisher).publishEvent(event);
    }

    @Test
    @DisplayName("Three warm days are not a spell yet")
    void threeWarmDaysAreNotASpell() {
        var today = LocalDate.of(2026, 7, 18);
        when(orchard.findPlotCentroid(any())).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(provider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any()))
                .thenReturn(Optional.of(winter(LocalDate.of(2026, 6, 1), today.minusDays(1), 3, 27.0)));

        var event = monitorOn(today).checkPlot(new PlotId(plotId), today).orElseThrow();

        assertEquals("NONE", event.thermalAnomaly());
        assertEquals(3, event.currentWarmStreakDays());
    }

    @Test
    @DisplayName("After the season a spell is never active, so open alerts get closed")
    void afterTheSeasonTheSpellIsNotActive() {
        var today = LocalDate.of(2026, 9, 3);
        when(orchard.findAllActivePlotIds()).thenReturn(List.of(plotId));
        when(orchard.findPlotCentroid(any())).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(provider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any()))
                .thenReturn(Optional.of(winter(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), 6, 27.0)));

        assertEquals(1, monitorOn(today).checkAllPlots());
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.<WinterWarmthEvaluatedEvent>argThat(
                event -> !event.isWarmSpellActive() && "RECORDED".equals(event.thermalAnomaly())));
    }

    @Test
    @DisplayName("Outside the season and its grace days nothing is evaluated")
    void outsideTheSeasonNothingRuns() {
        assertEquals(0, monitorOn(LocalDate.of(2026, 10, 14)).checkAllPlots());
        assertEquals(0, monitorOn(LocalDate.of(2026, 5, 31)).checkAllPlots());
        assertEquals(0, monitorOn(LocalDate.of(2026, 9, 8)).checkAllPlots());
        verify(orchard, never()).findAllActivePlotIds();
    }

    @Test
    @DisplayName("A plot without weather is skipped and does not stop the others")
    void plotWithoutWeatherIsSkipped() {
        var today = LocalDate.of(2026, 7, 18);
        when(orchard.findAllActivePlotIds()).thenReturn(List.of(plotId, UUID.randomUUID().toString()));
        when(orchard.findPlotCentroid(any())).thenReturn(Optional.of(new double[]{-18.0, -70.25}));
        when(provider.fetchHourlyTemperatures(anyDouble(), anyDouble(), any(), any())).thenReturn(Optional.empty());

        assertEquals(0, monitorOn(today).checkAllPlots());
        verify(publisher, never()).publishEvent(any(Object.class));
    }
}
