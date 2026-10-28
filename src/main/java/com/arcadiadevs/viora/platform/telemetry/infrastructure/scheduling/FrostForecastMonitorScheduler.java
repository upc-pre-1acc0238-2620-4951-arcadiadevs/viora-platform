package com.arcadiadevs.viora.platform.telemetry.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices.FrostForecastMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the {@link FrostForecastMonitor} two minutes after the platform starts and then every three hours.
 *
 * <p>Like the winter warmth monitor, the first run matters on a host that sleeps when idle (Render free tier).
 * Disable it with {@code viora.telemetry.frost-monitor.enabled=false} (the tests do, so they never call the weather
 * service).</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.telemetry.frost-monitor.enabled", havingValue = "true", matchIfMissing = true)
public class FrostForecastMonitorScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(FrostForecastMonitorScheduler.class);

    private final FrostForecastMonitor monitor;

    public FrostForecastMonitorScheduler(FrostForecastMonitor monitor) {
        this.monitor = monitor;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT3H")
    public void run() {
        try {
            int evaluated = monitor.checkAllPlots();
            if (evaluated > 0) {
                LOG.info("Frost forecast evaluated for {} plots", evaluated);
            }
        } catch (RuntimeException exception) {
            LOG.warn("Frost forecast monitor failed: {}", exception.getMessage());
        }
    }
}
