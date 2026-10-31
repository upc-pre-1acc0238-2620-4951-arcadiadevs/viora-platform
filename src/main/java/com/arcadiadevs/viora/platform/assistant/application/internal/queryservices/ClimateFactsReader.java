package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * Reads the figures of the climate screen: the thresholds of Telemetry, the forecast, the nodes of the plot and their
 * latest reading, only when it is recent.
 */
@Service
public class ClimateFactsReader implements ScreenFactsReader {

    /** A reading older than this is not shown as what the sensors say now. */
    static final Duration RECENT_READING = Duration.ofHours(24);

    private final ExternalTelemetryService externalTelemetryService;
    private final Clock clock;

    /**
     * Constructs the reader.
     *
     * @param externalTelemetryService reads the thresholds, the forecast and the nodes
     * @param clock                    the application clock, to tell a recent reading
     */
    public ClimateFactsReader(ExternalTelemetryService externalTelemetryService, Clock clock) {
        this.externalTelemetryService = externalTelemetryService;
        this.clock = clock;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.CLIMATE;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var sensors = externalTelemetryService.findSensors(plotId);
        var recent = sensors.readAt() != null
                && !sensors.readAt().isBefore(clock.instant().minus(RECENT_READING));
        return new ClimateFacts(externalTelemetryService.findThresholds(),
                externalTelemetryService.findForecast(plotId).orElse(null), sensors.nodes(),
                recent ? sensors.reading() : null);
    }
}
