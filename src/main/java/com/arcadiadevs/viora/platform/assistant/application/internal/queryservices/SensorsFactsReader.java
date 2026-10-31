package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Reads the figures of the sensors screen: the nodes of the plot, their latest reading only when it is recent, as the
 * climate screen reads it, and on how many of the last days the plot had readings. It reads no forecast.
 */
@Service
public class SensorsFactsReader implements ScreenFactsReader {

    /** Days, today included, the screen counts readings over: the week Telemetry keeps the latest one from. */
    static final int COVERAGE_DAYS = 7;

    private final ExternalTelemetryService externalTelemetryService;
    private final Clock clock;

    /**
     * Constructs the reader.
     *
     * @param externalTelemetryService reads the nodes, their latest reading and the days with readings
     * @param clock                    the application clock, to tell a recent reading and the days of the week
     */
    public SensorsFactsReader(ExternalTelemetryService externalTelemetryService, Clock clock) {
        this.externalTelemetryService = externalTelemetryService;
        this.clock = clock;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.SENSORS;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var sensors = externalTelemetryService.findSensors(plotId);
        var now = clock.instant();
        var recent = sensors.readAt() != null
                && !sensors.readAt().isBefore(now.minus(ClimateFactsReader.RECENT_READING));
        var today = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE));
        var from = today.minusDays(COVERAGE_DAYS - 1L).atStartOfDay(ProducerTimeZone.ZONE).toInstant();
        int days = externalTelemetryService.countDaysWithReadings(plotId, from, now);
        var lastReadingOn = sensors.readAt() == null
                ? null : LocalDate.ofInstant(sensors.readAt(), ProducerTimeZone.ZONE);
        return new SensorsFacts(sensors.nodes(), recent ? sensors.reading() : null,
                new ClimateData.Coverage(lastReadingOn, days, COVERAGE_DAYS));
    }
}
