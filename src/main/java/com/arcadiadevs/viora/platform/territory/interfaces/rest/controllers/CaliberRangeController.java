package com.arcadiadevs.viora.platform.territory.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.territory.application.commandservices.CaliberRangeSetCommandService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CaliberRangeSetQueryService;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCaliberRangesByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CaliberRangeResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.ReplaceCaliberRangesResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.transform.CaliberRangeResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the caliber range set aggregate: the caliber classes the plant of the manager's license sorts
 * olives by, so the intake projection splits the same way.
 */
@RestController
@RequestMapping(value = "/api/v1/caliber-ranges", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Caliber Ranges", description = "Caliber classes the plant of a cooperative license sorts olives by")
public class CaliberRangeController {

    private final CaliberRangeSetCommandService caliberRangeSetCommandService;
    private final CaliberRangeSetQueryService caliberRangeSetQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param caliberRangeSetCommandService the replace use case
     * @param caliberRangeSetQueryService   the read of the ranges
     * @param currentUserProvider           the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public CaliberRangeController(CaliberRangeSetCommandService caliberRangeSetCommandService,
                                  CaliberRangeSetQueryService caliberRangeSetQueryService,
                                  CurrentUserProvider currentUserProvider) {
        if (caliberRangeSetCommandService == null || caliberRangeSetQueryService == null
                || currentUserProvider == null) {
            throw new IllegalArgumentException("territory.caliber_range_controller.dependency.null");
        }
        this.caliberRangeSetCommandService = caliberRangeSetCommandService;
        this.caliberRangeSetQueryService = caliberRangeSetQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the caliber ranges of the license the signed-in manager is in charge of.
     *
     * @return 200 with the ranges, empty when none were set
     */
    @GetMapping
    @Operation(summary = "List the caliber ranges of the license",
            description = "From the biggest fruit to the smallest; empty while the plant set none.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The ranges",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = CaliberRangeResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a technical manager, or a manager without a "
                    + "license in force (reason territory.manager.no_license)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> list() {
        var result = caliberRangeSetQueryService.handle(
                new GetCaliberRangesByManagerIdQuery(currentUserProvider.requireCurrentUserId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                ranges -> ranges.stream().map(CaliberRangeResourceAssembler::toResource).toList(), HttpStatus.OK);
    }

    /**
     * Replaces the caliber ranges of the license the signed-in manager is in charge of.
     *
     * @param resource the new ranges
     * @return 200 with the ranges stored, or a problem detail naming the range in conflict
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replace the caliber ranges of the license",
            description = "The ranges go from the biggest fruit to the smallest, one right after the other, from 60 "
                    + "to 2000 fruits per kilogram. An inverted range, one that overlaps the previous one or one "
                    + "that leaves a gap after it is refused with 400, naming its position in rangeIndex and why in "
                    + "reason; the ranges in force stay. Later intake projections split by these ranges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The ranges stored",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = CaliberRangeResource.class)))),
            @ApiResponse(responseCode = "400", description = "A range is incomplete, off the scale, inverted, "
                    + "overlapping or leaves a gap (reason and rangeIndex)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Not a technical manager, or a manager without a "
                    + "license in force",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> replace(@Valid @RequestBody ReplaceCaliberRangesResource resource) {
        var result = caliberRangeSetCommandService.handle(CaliberRangeResourceAssembler.toCommandFromResource(
                currentUserProvider.requireCurrentUserId(), resource));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                set -> set.snapshot().ranges().stream().map(CaliberRangeResourceAssembler::toResource).toList(),
                HttpStatus.OK);
    }
}
