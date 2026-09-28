package com.arcadiadevs.viora.platform.phenology.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.PhenologyMetricQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetPlotMetricsQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.MetricType;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.resources.MetricResource;
import com.arcadiadevs.viora.platform.phenology.interfaces.rest.transform.MetricResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for querying biological phenological metrics and bearing indexes.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Phenology Metrics", description = "Endpoints for biological phenological metrics, Hoblyn BBI, and Erez chilling portions")
@NullMarked
public class PhenologyMetricController {

    private final PhenologyMetricQueryService metricQueryService;

    /**
     * Constructs the controller injecting the query service.
     *
     * @param metricQueryService the query service evaluating plot metrics
     */
    public PhenologyMetricController(PhenologyMetricQueryService metricQueryService) {
        this.metricQueryService = metricQueryService;
    }

    /**
     * Retrieves biological phenological metrics for an olive orchard plot, supporting optional metric filtering.
     *
     * @param plotId     the UUID of the olive plot
     * @param metricName optional metric filter parameter (e.g., BBI, CHILLING)
     * @param name       optional alias filter parameter matching early specification
     * @return 200 OK with list of metrics, 400 Bad Request on invalid arguments, or 404 Not Found if plot does not exist
     */
    @GetMapping
    @Operation(
            summary = "Get plot phenological metrics",
            description = "Calculates and returns biological indicators (Hoblyn BBI and Erez Dynamic chilling portions) for an olive plot"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Plot metrics successfully evaluated and retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MetricResource.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid plot UUID format or unrecognized metric filter",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Plot not found or inactive in orchard registry, or tracker not initialized",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))
            )
    })
    public ResponseEntity<?> getPlotMetrics(
            @PathVariable @Parameter(description = "Plot UUID", required = true) String plotId,
            @RequestParam(name = "metricName", required = false) @Nullable String metricName,
            @RequestParam(name = "name", required = false) @Nullable String name
    ) {
        PlotId targetPlotId;
        try {
            targetPlotId = new PlotId(plotId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.failure(ApplicationError.validationError("plotId", ex.getMessage())),
                    MetricResourceFromEntityAssembler::toResourceList,
                    HttpStatus.OK
            );
        }

        String effectiveMetricParam = metricName != null && !metricName.isBlank() ? metricName : name;
        MetricType filterType = null;
        if (effectiveMetricParam != null && !effectiveMetricParam.isBlank()) {
            try {
                filterType = MetricType.fromString(effectiveMetricParam);
            } catch (IllegalArgumentException ex) {
                return ResponseEntityAssembler.toResponseEntityFromResult(
                        Result.failure(ApplicationError.validationError("metricName", ex.getMessage())),
                        MetricResourceFromEntityAssembler::toResourceList,
                        HttpStatus.OK
                );
            }
        }

        var query = new GetPlotMetricsQuery(targetPlotId, filterType);
        var result = metricQueryService.handle(query);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                MetricResourceFromEntityAssembler::toResourceList,
                HttpStatus.OK
        );
    }
}
