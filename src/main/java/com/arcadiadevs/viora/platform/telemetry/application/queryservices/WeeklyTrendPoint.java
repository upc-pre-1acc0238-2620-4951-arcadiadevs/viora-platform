package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import java.time.Instant;

/**
 * Single data point for weekly trend visualization in incident details.
 *
 * @param timestamp the observation timestamp
 * @param value     the recorded or forecast value
 * @param threshold the threshold value for comparison
 */
public record WeeklyTrendPoint(
        Instant timestamp,
        Double value,
        Double threshold
) {
}
