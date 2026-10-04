package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.interfaces.rest.resources.MessageResource;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.AgroclimaticIncidentQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.*;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.AgroclimaticIncidentResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.CompleteMitigationStepCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.PostponeAgroclimaticIncidentCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for agroclimatic and edaphic incident alerts and mitigations.
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Agroclimatic Incidents", description = "Endpoints for agroclimatic incident alerts, mitigations, and trends")
@NullMarked
public class AgroclimaticIncidentController {

    private final AgroclimaticIncidentQueryService queryService;
    private final AgroclimaticIncidentCommandService commandService;
    private final @Nullable MessageSource messageSource;

    /**
     * Constructs the controller injecting query, command services, and message source.
     *
     * @param queryService   the incident query service
     * @param commandService the incident command service
     * @param messageSource  the i18n message source for localized mitigations
     */
    @Autowired
    public AgroclimaticIncidentController(
            AgroclimaticIncidentQueryService queryService,
            AgroclimaticIncidentCommandService commandService,
            @Nullable MessageSource messageSource
    ) {
        if (queryService == null) {
            throw new IllegalArgumentException("incident.query_service.null");
        }
        if (commandService == null) {
            throw new IllegalArgumentException("incident.command_service.null");
        }
        this.queryService = queryService;
        this.commandService = commandService;
        this.messageSource = messageSource;
    }

    /**
     * Overload for testing without an explicit MessageSource.
     *
     * @param queryService   the incident query service
     * @param commandService the incident command service
     */
    public AgroclimaticIncidentController(
            AgroclimaticIncidentQueryService queryService,
            AgroclimaticIncidentCommandService commandService
    ) {
        this(queryService, commandService, null);
    }

    /**
     * Lists agroclimatic incidents with optional filters.
     *
     * @param plotId   optional plot UUID
     * @param status   optional status
     * @param severity optional severity
     * @return 200 OK with summary and incident list
     */
    @GetMapping("/api/v1/agroclimatic-incidents")
    @Operation(summary = "List all agroclimatic incidents", description = "Retrieves summarized incidents with counter metrics, filterable by plot, status, and severity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidents summary successfully retrieved",
                    content = @Content(schema = @Schema(implementation = AgroclimaticIncidentsSummaryResource.class)))
    })
    public ResponseEntity<?> listIncidents(
            @Parameter(description = "Optional plot UUID filter") @RequestParam(required = false) @Nullable String plotId,
            @Parameter(description = "Optional status filter (ACTIVE, SNOOZED, NORMALIZED)") @RequestParam(required = false) @Nullable String status,
            @Parameter(description = "Optional severity filter (WARNING, CRITICAL)") @RequestParam(required = false) @Nullable String severity
    ) {
        var query = new GetAgroclimaticIncidentsQuery(plotId, status, severity);
        var result = queryService.handle(query);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                AgroclimaticIncidentResourceFromEntityAssembler::toSummaryResource,
                HttpStatus.OK
        );
    }

    /**
     * Lists agroclimatic incidents for a specific plot.
     *
     * @param plotId   target plot UUID
     * @param status   optional status
     * @param severity optional severity
     * @return 200 OK with summary and incident list
     */
    @GetMapping("/api/v1/plots/{plotId}/agroclimatic-incidents")
    @Operation(summary = "List agroclimatic incidents for a specific plot", description = "Retrieves incidents scoped to the given plot.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plot incidents successfully retrieved",
                    content = @Content(schema = @Schema(implementation = AgroclimaticIncidentsSummaryResource.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found")
    })
    public ResponseEntity<?> listPlotIncidents(
            @Parameter(description = "Target plot UUID") @PathVariable String plotId,
            @Parameter(description = "Optional status filter (ACTIVE, SNOOZED, NORMALIZED)") @RequestParam(required = false) @Nullable String status,
            @Parameter(description = "Optional severity filter (WARNING, CRITICAL)") @RequestParam(required = false) @Nullable String severity
    ) {
        var query = new GetAgroclimaticIncidentsQuery(plotId, status, severity);
        var result = queryService.handle(query);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                AgroclimaticIncidentResourceFromEntityAssembler::toSummaryResource,
                HttpStatus.OK
        );
    }

    /**
     * Retrieves detailed information of an incident including action steps and weekly trend.
     *
     * @param incidentId target incident UUID
     * @return 200 OK with detail representation
     */
    @GetMapping("/api/v1/agroclimatic-incidents/{incidentId}")
    @Operation(summary = "Get detailed agroclimatic incident information", description = "Retrieves full details for an incident including action steps and weekly trend data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident details successfully retrieved",
                    content = @Content(schema = @Schema(implementation = AgroclimaticIncidentDetailResource.class))),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<?> getIncidentDetail(
            @Parameter(description = "Target incident UUID") @PathVariable String incidentId
    ) {
        var query = new GetAgroclimaticIncidentByIdQuery(incidentId);
        var result = queryService.handle(query);
        var locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                detail -> AgroclimaticIncidentResourceFromEntityAssembler.toDetailResource(detail, messageSource, locale),
                HttpStatus.OK
        );
    }

    /**
     * Postpones (snoozes) an active incident.
     *
     * @param incidentId target incident UUID
     * @param resource   postponement payload
     * @return 200 OK on success
     */
    @PostMapping("/api/v1/agroclimatic-incidents/{incidentId}/postponements")
    @Operation(summary = "Postpone (snooze) an active incident", description = "Creates a postponement sub-resource to snooze notifications for an incident.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident successfully snoozed",
                    content = @Content(schema = @Schema(implementation = MessageResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid duration"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<?> postponeIncident(
            @Parameter(description = "Target incident UUID") @PathVariable String incidentId,
            @Valid @RequestBody PostponeIncidentResource resource
    ) {
        var command = PostponeAgroclimaticIncidentCommandFromResourceAssembler.toCommand(incidentId, resource);
        var result = commandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                id -> new MessageResource("incident.postponed_successfully"),
                HttpStatus.OK
        );
    }

    /**
     * Completes an actionable mitigation step within an incident.
     *
     * @param incidentId parent incident UUID
     * @param stepId     target step UUID
     * @param resource   optional step payload
     * @return 200 OK on success
     */
    @PutMapping("/api/v1/agroclimatic-incidents/{incidentId}/mitigation-steps/{stepId}")
    @Operation(summary = "Complete an actionable mitigation step", description = "Marks a specific mitigation task step within an incident as completed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mitigation step successfully completed",
                    content = @Content(schema = @Schema(implementation = MessageResource.class))),
            @ApiResponse(responseCode = "404", description = "Incident or step not found")
    })
    public ResponseEntity<?> completeMitigationStep(
            @Parameter(description = "Parent incident UUID") @PathVariable String incidentId,
            @Parameter(description = "Target step UUID") @PathVariable String stepId,
            @RequestBody(required = false) @Nullable CompleteMitigationStepResource resource
    ) {
        var command = CompleteMitigationStepCommandFromResourceAssembler.toCommand(incidentId, stepId);
        var result = commandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                id -> new MessageResource("mitigation_step.completed_successfully"),
                HttpStatus.OK
        );
    }
}
