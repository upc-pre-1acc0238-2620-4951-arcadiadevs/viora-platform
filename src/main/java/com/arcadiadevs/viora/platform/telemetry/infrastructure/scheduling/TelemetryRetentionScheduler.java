package com.arcadiadevs.viora.platform.telemetry.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Deletes the hourly readings older than the retention, once a day, so the series of a plot does not grow forever.
 *
 * <p>The app shows the last week, and the climate screens at most a few weeks; {@code viora.telemetry.retention-days}
 * (90 by default) keeps far more than that. Disable it with {@code viora.telemetry.retention.enabled=false}.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.telemetry.retention.enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryRetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryRetentionScheduler.class);

    private final TelemetrySeriesRepository seriesRepository;
    private final Clock clock;
    private final Duration retention;

    public TelemetryRetentionScheduler(TelemetrySeriesRepository seriesRepository, Clock clock,
            @Value("${viora.telemetry.retention-days:90}") long retentionDays) {
        if (retentionDays < 8) {
            throw new IllegalArgumentException("telemetry.retention_days.too_short");
        }
        this.seriesRepository = seriesRepository;
        this.clock = clock;
        this.retention = Duration.ofDays(retentionDays);
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT24H")
    @Transactional
    public void run() {
        try {
            int deleted = seriesRepository.deleteReadingsOlderThan(clock.instant().minus(retention));
            if (deleted > 0) {
                LOG.info("Deleted {} telemetry readings older than {} days", deleted, retention.toDays());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Telemetry retention failed: {}", exception.getMessage());
        }
    }
}
