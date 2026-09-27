package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

/**
 * Domain query record for retrieving all IoT devices bound to a specific plot.
 *
 * @param plotId the unique plot identifier value object
 */
public record GetIoTDevicesByPlotIdQuery(PlotId plotId) {

    /**
     * Compact constructor validating that plotId is not null.
     *
     * @param plotId the plot identifier value object
     */
    public GetIoTDevicesByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
    }

    /**
     * Convenience factory constructor from a raw UUID string.
     *
     * @param rawPlotId the raw UUID string of the plot
     */
    public GetIoTDevicesByPlotIdQuery(String rawPlotId) {
        this(new PlotId(rawPlotId));
    }
}
