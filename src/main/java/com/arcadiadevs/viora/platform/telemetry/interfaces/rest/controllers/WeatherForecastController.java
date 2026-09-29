package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.WeatherForecastQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.WeatherForecastResource;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.WeatherForecastResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for 7-day agroclimatic weather forecast projections.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/forecasts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Weather Forecasts", description = "Endpoints for 7-day agroclimatic and frost risk weather forecasts")
@NullMarked
public class WeatherForecastController {

    private final WeatherForecastQueryService weatherForecastQueryService;

    public WeatherForecastController(WeatherForecastQueryService weatherForecastQueryService) {
        if (weatherForecastQueryService == null) {
            throw new IllegalArgumentException("telemetry.query_service.null");
        }
        this.weatherForecastQueryService = weatherForecastQueryService;
    }

    /**
     * Retrieves the 7-day weather forecast projection for an active orchard plot.
     *
     * @param plotId the plot UUID string
     * @return 200 OK with the WeatherForecastResource, or 404 ProblemDetail if plot does not exist or is inactive
     */
    @GetMapping
    @Operation(
            summary = "Query 7-day agroclimatic weather forecast",
            description = "Retrieves georeferenced 7-day weather forecast projections for an active plot, including daily temperatures, frost risk alerts (< 2.0 °C), precipitation probability, and wind speed."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Weather forecast projections retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WeatherForecastResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID format"),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active")
    })
    public ResponseEntity<?> getWeatherForecast(
            @Parameter(description = "Plot unique identifier UUID", required = true)
            @PathVariable String plotId
    ) {
        var query = new GetWeatherForecastByPlotIdQuery(new PlotId(plotId));
        var result = weatherForecastQueryService.handle(query);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                WeatherForecastResourceAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }
}
