package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.TelemetryResource;

import java.util.UUID;

/**
 * Assembler mapping domain {@link HourlyTelemetryReadingSnapshot} models to {@link TelemetryResource} DTOs.
 */
public final class TelemetryResourceFromEntityAssembler {

    private TelemetryResourceFromEntityAssembler() {
    }

    /**
     * Converts a reading snapshot and referenced plot UUID into a presentation resource.
     *
     * @param snapshot the hourly reading snapshot
     * @param plotId   the plot UUID string
     * @return the mapped {@link TelemetryResource}
     */
    public static TelemetryResource toResource(HourlyTelemetryReadingSnapshot snapshot, String plotId) {
        if (snapshot == null) {
            throw new IllegalArgumentException("telemetry.reading.snapshot.null");
        }
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }

        return new TelemetryResource(
                UUID.fromString(snapshot.id().readingId()),
                UUID.fromString(plotId),
                snapshot.temperature().celsius(),
                snapshot.relativeHumidity().percentage(),
                snapshot.soilMoisture().percentage(),
                snapshot.solarRadiation().wattsPerSquareMeter(),
                snapshot.stemWaterPotential() != null ? snapshot.stemWaterPotential().stemWaterPotentialMpa() : null,
                snapshot.timestamp().timestamp()
        );
    }
}
