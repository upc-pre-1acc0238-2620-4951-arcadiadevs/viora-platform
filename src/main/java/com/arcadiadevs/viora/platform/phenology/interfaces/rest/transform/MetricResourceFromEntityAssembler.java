package com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricEvaluationResult;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.MetricResource;

import java.util.List;

/**
 * Assembler converting domain metric evaluation results into public {@link MetricResource} instances.
 */
public final class MetricResourceFromEntityAssembler {

    private MetricResourceFromEntityAssembler() {
    }

    /**
     * Converts a single domain evaluation result into a REST presentation resource.
     *
     * @param result the domain metric evaluation result
     * @return the mapped {@link MetricResource}
     */
    public static MetricResource toResource(MetricEvaluationResult result) {
        if (result == null) {
            return null;
        }
        return new MetricResource(
                result.metricType().name(),
                result.value(),
                result.qualitativeCategory(),
                result.details(),
                result.evaluatedAt()
        );
    }

    /**
     * Converts a list of domain evaluation results into a list of REST presentation resources.
     *
     * @param results the list of domain evaluation results
     * @return the list of mapped {@link MetricResource} instances
     */
    public static List<MetricResource> toResourceList(List<MetricEvaluationResult> results) {
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .map(MetricResourceFromEntityAssembler::toResource)
                .toList();
    }
}
