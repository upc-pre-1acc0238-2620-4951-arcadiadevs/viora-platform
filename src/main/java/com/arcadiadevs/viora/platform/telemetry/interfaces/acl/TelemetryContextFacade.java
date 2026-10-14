package com.arcadiadevs.viora.platform.telemetry.interfaces.acl;

import java.time.Instant;

/**
 * ACL facade exposing Telemetry bounded context capabilities to external contexts.
 *
 * <p>Only primitive, published-language structures cross the boundary; Telemetry aggregates and value objects stay
 * internal.</p>
 */
public interface TelemetryContextFacade {

    /**
     * Summarizes how much climate data a plot has over a period, for the agronomic dossier.
     *
     * @param plotId the plot identifier string (UUID)
     * @param from   start of the period, inclusive
     * @param to     end of the period, inclusive
     * @return the linked nodes and the days with at least one hourly reading; zeros when there is none
     */
    TelemetryCoverage findTelemetryCoverage(String plotId, Instant from, Instant to);

    /**
     * Climate data available for a plot over a period.
     *
     * @param linkedNodes      nodes linked to the plot and not unlinked
     * @param daysWithReadings distinct UTC days with at least one hourly reading
     */
    record TelemetryCoverage(int linkedNodes, int daysWithReadings) {
    }
}
