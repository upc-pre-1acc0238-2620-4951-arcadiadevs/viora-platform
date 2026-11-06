package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.PlotSectorAssignmentQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ResolvePlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentByPlotIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentsByUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.AssignPlotSectorResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.PlotSectorAssignmentResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.ResolvePlotSectorResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.PlotSectorAssignmentResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

/**
 * REST controller of the plot sector assignment aggregate: the sector of each member plot, identified by its plot.
 * <p>A member gives their plots a sector; the manager sees every member plot and reviews the ones «Por confirmar».
 * Another license's plot, or another member's, answers 404.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/plot-sector-assignments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Plot Sector Assignments", description = "Sector of each member plot within its license")
public class PlotSectorAssignmentController {

    private final PlotSectorAssignmentCommandService plotSectorAssignmentCommandService;
    private final PlotSectorAssignmentQueryService plotSectorAssignmentQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param plotSectorAssignmentCommandService the assign and review use cases
     * @param plotSectorAssignmentQueryService   the plot sector reads
     * @param currentUserProvider                the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public PlotSectorAssignmentController(PlotSectorAssignmentCommandService plotSectorAssignmentCommandService,
                                          PlotSectorAssignmentQueryService plotSectorAssignmentQueryService,
                                          CurrentUserProvider currentUserProvider) {
        if (plotSectorAssignmentCommandService == null || plotSectorAssignmentQueryService == null
                || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.plot_sector_controller.dependency.null");
        }
        this.plotSectorAssignmentCommandService = plotSectorAssignmentCommandService;
        this.plotSectorAssignmentQueryService = plotSectorAssignmentQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the plot sectors the signed-in user reaches.
     *
     * @param status only {@code pending} («Por confirmar») or {@code assigned}; all when left out
     * @return 200 with the plot sectors, the ones «Por confirmar» first
     */
    @GetMapping
    @Operation(
            summary = "List the sectors of member plots",
            description = "A technical manager gets every member plot of the license; a member, their own plots. "
                    + "A plot without a row has no sector informed.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The plot sectors",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = PlotSectorAssignmentResource.class)))),
            @ApiResponse(responseCode = "400", description = "Unknown status"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user reaches no license")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "pending or assigned") @RequestParam(required = false) String status) {
        PlotSectorStatus filter = null;
        if (status != null) {
            var match = Arrays.stream(PlotSectorStatus.values())
                    .filter(each -> each.name().equalsIgnoreCase(status.trim()))
                    .findFirst();
            if (match.isEmpty()) {
                return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.validationError("status", "territory.plot_sector.status.unknown"));
            }
            filter = match.get();
        }
        var result = plotSectorAssignmentQueryService.handle(
                new GetPlotSectorAssignmentsByUserIdQuery(currentUserProvider.requireCurrentUserId(), filter));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                assignments -> assignments.stream().map(PlotSectorAssignmentResourceAssembler::toResource).toList(),
                HttpStatus.OK);
    }

    /**
     * Reads the sector of one plot the signed-in user reaches.
     *
     * @param plotId the plot
     * @return 200 with its sector, or 404 when it has none or the user does not reach it
     */
    @GetMapping("/{plotId}")
    @Operation(summary = "Get the sector of a member plot", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The plot sector",
                    content = @Content(schema = @Schema(implementation = PlotSectorAssignmentResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user reaches no license"),
            @ApiResponse(responseCode = "404", description = "The plot has no sector, or the user does not reach it")
    })
    public ResponseEntity<?> get(@PathVariable UUID plotId) {
        var result = plotSectorAssignmentQueryService.handle(new GetPlotSectorAssignmentByPlotIdAndUserIdQuery(
                plotId.toString(), currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                PlotSectorAssignmentResourceAssembler::toResource, HttpStatus.OK);
    }

    /**
     * Gives one of the signed-in member's plots a sector.
     *
     * @param plotId   the plot
     * @param resource a sector of the list, or the name the member writes
     * @return 200 with the plot sector, or a problem detail explaining why it was not given
     */
    @PutMapping(value = "/{plotId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Give a plot a sector",
            description = "Send a sector of the list, or the name of one it does not have. A written name that "
                    + "matches the list, ignoring accents and case, takes that sector; otherwise the plot stays "
                    + "«Por confirmar» until the manager reviews it, and it is saved anyway.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sector given",
                    content = @Content(schema = @Schema(implementation = PlotSectorAssignmentResource.class))),
            @ApiResponse(responseCode = "400", description = "Neither or both fields, or an invalid name"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "A producer without a sponsored plan (reason "
                    + "territory.producer.not_sponsored)"),
            @ApiResponse(responseCode = "404", description = "Not an active plot of the member, or no such sector "
                    + "in the license")
    })
    public ResponseEntity<?> assign(@PathVariable UUID plotId, @RequestBody AssignPlotSectorResource resource) {
        var userId = currentUserProvider.requireCurrentUserId();
        var result = plotSectorAssignmentCommandService.handle(new AssignPlotSectorCommand(userId, plotId.toString(),
                resource.sectorZoneId(), resource.name()));
        return respondWithView(result.failure().orElse(null), plotId, userId);
    }

    /**
     * Reviews a plot «Por confirmar» of the signed-in manager's license.
     *
     * @param plotId   the plot
     * @param resource the existing sector it means, or nothing to add the written name as a sector
     * @return 200 with the plot sector, or a problem detail explaining why it was not reviewed
     */
    @PostMapping("/{plotId}/resolutions")
    @Operation(
            summary = "Review a plot «Por confirmar»",
            description = "Without a sector, the written name is added to the list («Añadir como sector»); with "
                    + "one, the plot joins it («Es el mismo que…»). Every plot where the same name was written is "
                    + "resolved the same way. The member's plot itself does not change.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plot reviewed",
                    content = @Content(schema = @Schema(implementation = PlotSectorAssignmentResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a manager with a license in force"),
            @ApiResponse(responseCode = "404", description = "No such plot or sector in the license"),
            @ApiResponse(responseCode = "409", description = "The plot is no longer «Por confirmar» (reason "
                    + "territory.plot_sector.not_pending)")
    })
    public ResponseEntity<?> resolve(@PathVariable UUID plotId,
                                     @RequestBody(required = false) ResolvePlotSectorResource resource) {
        var userId = currentUserProvider.requireCurrentUserId();
        var sectorZoneId = resource == null ? null : resource.sectorZoneId();
        var result = plotSectorAssignmentCommandService.handle(
                new ResolvePlotSectorCommand(userId, plotId.toString(), sectorZoneId));
        return respondWithView(result.failure().orElse(null), plotId, userId);
    }

    private ResponseEntity<?> respondWithView(ApplicationError failure, UUID plotId, String userId) {
        if (failure != null) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(failure);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                plotSectorAssignmentQueryService.handle(
                        new GetPlotSectorAssignmentByPlotIdAndUserIdQuery(plotId.toString(), userId)),
                PlotSectorAssignmentResourceAssembler::toResource, HttpStatus.OK);
    }
}
