package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeCodeQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeCodePreviewQuery;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeCodePreviewResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.PreviewCooperativeCodeResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.CooperativeCodePreviewResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller where a producer previews a cooperative code before redeeming it: who sponsors the plan, who would
 * read their data and how their hectares compare with the quota. Previewing does not consume the code.
 * <p>It is a POST so the code travels in the body and never in a URL or a log.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/cooperative-code-previews", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cooperative Code Previews", description = "What a cooperative code offers, before redeeming it")
public class CooperativeCodePreviewController {

    private final CooperativeCodeQueryService cooperativeCodeQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param cooperativeCodeQueryService the code preview
     * @param currentUserProvider         the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeCodePreviewController(CooperativeCodeQueryService cooperativeCodeQueryService,
                                            CurrentUserProvider currentUserProvider) {
        if (cooperativeCodeQueryService == null || currentUserProvider == null) {
            throw new IllegalArgumentException("subscription.code_preview_controller.dependency.null");
        }
        this.cooperativeCodeQueryService = cooperativeCodeQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Previews a cooperative code for the authenticated producer.
     *
     * @param resource the code the cooperative gave the producer
     * @return 200 with the preview, or a problem detail explaining why the code cannot be redeemed
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Preview a cooperative invitation code",
            description = "Answers the organization, the manager, the quota, the end of the plan, the producer's "
                    + "registered hectares and the version of the consent text, without consuming the code. Send "
                    + "that version back when redeeming to accept sharing the data.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "What the code offers",
                    content = @Content(schema = @Schema(implementation = CooperativeCodePreviewResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload, or a code that does not exist, "
                    + "expired, was redeemed or revoked (reason subscription.invitation_code.not_redeemable)"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer"),
            @ApiResponse(responseCode = "409", description = "The producer already has an active subscription "
                    + "(reason subscription.producer.already_active)"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; see Retry-After")
    })
    public ResponseEntity<?> preview(@Valid @RequestBody PreviewCooperativeCodeResource resource) {
        var query = new GetCooperativeCodePreviewQuery(currentUserProvider.requireCurrentUserId(), resource.code());
        var result = cooperativeCodeQueryService.handle(query);
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                CooperativeCodePreviewResourceAssembler::toResource, HttpStatus.OK);
    }
}
