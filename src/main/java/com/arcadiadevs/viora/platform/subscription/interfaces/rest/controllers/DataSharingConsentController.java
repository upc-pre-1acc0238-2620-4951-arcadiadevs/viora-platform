package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.DataSharingConsentCommandService;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.DataSharingConsentQueryService;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.WithdrawDataSharingConsentCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentByIdAndProducerIdQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetDataSharingConsentsByProducerIdQuery;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.DataSharingConsentResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform.DataSharingConsentResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller of the data sharing consent aggregate, as the producer who gave them sees it.
 * <p>The producer always comes from the session: a consent of another producer answers 404, the same as one that
 * does not exist.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/data-sharing-consents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Data Sharing Consents", description = "Consents of producers to share their data with their cooperative")
public class DataSharingConsentController {

    private final DataSharingConsentCommandService dataSharingConsentCommandService;
    private final DataSharingConsentQueryService dataSharingConsentQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param dataSharingConsentCommandService the withdrawal use case
     * @param dataSharingConsentQueryService   the consent reads
     * @param currentUserProvider              the authenticated user
     * @throws IllegalArgumentException if any dependency is null
     */
    public DataSharingConsentController(DataSharingConsentCommandService dataSharingConsentCommandService,
                                        DataSharingConsentQueryService dataSharingConsentQueryService,
                                        CurrentUserProvider currentUserProvider) {
        if (dataSharingConsentCommandService == null || dataSharingConsentQueryService == null
                || currentUserProvider == null) {
            throw new IllegalArgumentException("subscription.consent_controller.dependency.null");
        }
        this.dataSharingConsentCommandService = dataSharingConsentCommandService;
        this.dataSharingConsentQueryService = dataSharingConsentQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Lists the consents of the authenticated producer.
     *
     * @return 200 with the consents, newest first
     */
    @GetMapping
    @Operation(
            summary = "List the data sharing consents of the signed-in producer",
            description = "Each consent names the organization and the manager who reads the data now, and whether "
                    + "it is active, withdrawn or ended with its plan.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The consents",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = DataSharingConsentResource.class)))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer")
    })
    public ResponseEntity<List<DataSharingConsentResource>> list() {
        var query = new GetDataSharingConsentsByProducerIdQuery(currentUserProvider.requireCurrentUserId());
        return ResponseEntity.ok(dataSharingConsentQueryService.handle(query).stream()
                .map(DataSharingConsentResourceAssembler::toResource)
                .toList());
    }

    /**
     * Reads one consent of the authenticated producer.
     *
     * @param id the consent
     * @return 200 with the consent, or 404 if it is not one of the producer's
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a data sharing consent of the signed-in producer",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The consent",
                    content = @Content(schema = @Schema(implementation = DataSharingConsentResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer"),
            @ApiResponse(responseCode = "404", description = "No such consent among the producer's")
    })
    public ResponseEntity<DataSharingConsentResource> get(@PathVariable UUID id) {
        var query = new GetDataSharingConsentByIdAndProducerIdQuery(id.toString(),
                currentUserProvider.requireCurrentUserId());
        return dataSharingConsentQueryService.handle(query)
                .map(DataSharingConsentResourceAssembler::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Stops sharing the authenticated producer's data for good. The plan and its seat stay.
     *
     * @param id the consent
     * @return 200 with the withdrawn consent, or a problem detail explaining why it was not withdrawn
     */
    @PostMapping("/{id}/withdrawals")
    @Operation(
            summary = "Stop sharing data with the cooperative",
            description = "The manager stops reading the producer's data at once and for good. The sponsored plan "
                    + "and its hectares stay until it ends.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consent withdrawn",
                    content = @Content(schema = @Schema(implementation = DataSharingConsentResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "The user is not a producer"),
            @ApiResponse(responseCode = "404", description = "No such consent among the producer's"),
            @ApiResponse(responseCode = "409", description = "Already withdrawn or ended with its plan (reason "
                    + "subscription.consent.not_active)")
    })
    public ResponseEntity<?> withdraw(@PathVariable UUID id) {
        var producerId = currentUserProvider.requireCurrentUserId();
        var result = dataSharingConsentCommandService.handle(
                new WithdrawDataSharingConsentCommand(producerId, id.toString()));
        if (result.isFailure()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(result.failure().orElseThrow());
        }
        return dataSharingConsentQueryService.handle(
                        new GetDataSharingConsentByIdAndProducerIdQuery(id.toString(), producerId))
                .map(DataSharingConsentResourceAssembler::toResource)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
