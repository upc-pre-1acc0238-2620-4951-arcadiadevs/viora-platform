package com.arcadiadevs.viora.platform.telemetry.domain.model.queries;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;

/**
 * Domain query record for retrieving the 7-day weather forecast for a specific orchard plot.
 *
 * @param plotId the unique plot identifier value object
 */
public record GetWeatherForecastByPlotIdQuery(PlotId plotId) {

    /**
     * Compact constructor validating that plotId is not null.
     *
     * @param plotId the plot identifier value object
     */
    public GetWeatherForecastByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("device.plot_id.null_or_empty");
        }
    }

    /**
     * Convenience constructor creating the query from a raw plot UUID string.
     *
     * @param rawPlotId the raw UUID string of the plot
     */
    public GetWeatherForecastByPlotIdQuery(String rawPlotId) {
        this(new PlotId(rawPlotId));
    }
}
