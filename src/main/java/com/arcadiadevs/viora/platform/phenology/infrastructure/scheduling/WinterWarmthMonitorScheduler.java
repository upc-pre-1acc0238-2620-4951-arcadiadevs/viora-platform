package com.arcadiadevs.viora.platform.phenology.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.phenology.application.internal.commandservices.WinterWarmthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the {@link WinterWarmthMonitor} one minute after the platform starts and then every six hours.
 *
 * <p>The first run matters on a host that sleeps when idle (Render free tier): the platform wakes up with the first
 * request and evaluates the winter right away. Disable it with {@code viora.phenology.warm-winter-monitor.enabled=false}
 * (the tests do, so they never call the weather service).</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.phenology.warm-winter-monitor.enabled", havingValue = "true", matchIfMissing = true)
public class WinterWarmthMonitorScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(WinterWarmthMonitorScheduler.class);

    private final WinterWarmthMonitor monitor;

    public WinterWarmthMonitorScheduler(WinterWarmthMonitor monitor) {
        this.monitor = monitor;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT6H")
    public void run() {
        try {
            int evaluated = monitor.checkAllPlots();
            if (evaluated > 0) {
                LOG.info("Winter warmth evaluated for {} plots", evaluated);
            }
        } catch (RuntimeException exception) {
            LOG.warn("Winter warmth monitor failed: {}", exception.getMessage());
        }
    }
}
