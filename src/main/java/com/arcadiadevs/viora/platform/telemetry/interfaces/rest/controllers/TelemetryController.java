package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.TelemetryQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.TelemetryResource;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.TelemetryResourceFromEntityAssembler;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller exposing endpoints for agroclimatic and soil telemetry time-series observations.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/telemetries", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry Series", description = "Endpoints for agroclimatic microclimate and soil moisture telemetry observations")
@NullMarked
public class TelemetryController {

    private final TelemetryQueryService telemetryQueryService;

    /**
     * Constructs the TelemetryController injecting the query service port.
     *
     * @param telemetryQueryService the telemetry query service
     */
    public TelemetryController(TelemetryQueryService telemetryQueryService) {
        if (telemetryQueryService == null) {
            throw new IllegalArgumentException("telemetry.query_service.null");
        }
        this.telemetryQueryService = telemetryQueryService;
    }

    /**
     * Retrieves hourly telemetry observations for a plot within an optional date range.
     *
     * @param plotId    the plot UUID string
     * @param startDate optional ISO-8601 start instant filter
     * @param endDate   optional ISO-8601 end instant filter
     * @return 200 OK with list of observations, or 404 ProblemDetail if plot does not exist or is inactive
     */
    @GetMapping
    @Operation(
            summary = "Query agroclimatic and soil telemetry series",
            description = "Retrieves chronological hourly observations of air temperature, relative humidity, soil moisture, and solar radiation for an active plot."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Telemetry observations retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TelemetryResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID format or date range filter"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active")
    })
    public ResponseEntity<?> getTelemetrySeries(
            @Parameter(description = "Plot unique identifier UUID", required = true)
            @PathVariable String plotId,
            @Parameter(description = "Inclusive start instant filter (ISO-8601 UTC)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant startDate,
            @Parameter(description = "Inclusive end instant filter (ISO-8601 UTC)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant endDate
    ) {
        var query = new GetTelemetrySeriesByPlotIdQuery(
                new PlotId(plotId),
                startDate,
                endDate
        );

        var result = telemetryQueryService.handle(query);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                readings -> readings.stream()
                        .map(snap -> TelemetryResourceFromEntityAssembler.toResource(snap, plotId))
                        .toList(),
                HttpStatus.OK
        );
    }
}
