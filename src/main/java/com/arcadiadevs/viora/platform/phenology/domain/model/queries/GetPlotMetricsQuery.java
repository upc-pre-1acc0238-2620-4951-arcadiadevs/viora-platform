package com.arcadiadevs.viora.platform.phenology.domain.model.queries;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;

/**
 * Domain query record for retrieving biological and mathematical phenological metrics of an orchard plot.
 *
 * @param plotId     the logical reference to the target orchard plot
 * @param metricType the optional metric filter (if null, retrieves all available metrics)
 */
public record GetPlotMetricsQuery(
        PlotId plotId,
        MetricType metricType
) {

    /**
     * Compact constructor enforcing non-null plot reference.
     *
     * @param plotId     the target plot identifier
     * @param metricType optional metric type filter
     * @throws IllegalArgumentException if plotId is null
     */
    public GetPlotMetricsQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor mapping raw String parameters to typed Value Objects.
     *
     * @param plotId    the raw plot UUID string
     * @param rawMetric optional raw metric string (e.g. "BBI", "CHILLING")
     */
    public GetPlotMetricsQuery(String plotId, String rawMetric) {
        this(
                new PlotId(plotId),
                rawMetric != null && !rawMetric.isBlank() ? MetricType.fromString(rawMetric) : null
        );
    }
}
