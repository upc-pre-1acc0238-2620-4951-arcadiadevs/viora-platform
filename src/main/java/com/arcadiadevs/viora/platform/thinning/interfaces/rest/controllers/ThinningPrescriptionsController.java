package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.thinning.application.internal.queryservices.ThinningPrescriptionOutlookService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningPrescriptionQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.EvaluateThinningPrescriptionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionByIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PrescriptionResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform.PrescriptionResourceFromEntityAssembler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;

/**
 * REST controller reading the thinning prescriptions of the producer across plots: the thinning plan (P60)
 * lists them all and a prescription can be read by its identifier.
 */
@RestController
@RequestMapping(value = "/api/v1/thinning-prescriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Thinning Prescriptions", description = "Thinning prescriptions of the producer across plots")
@NullMarked
public class ThinningPrescriptionsController {

    private static final Logger LOG = LoggerFactory.getLogger(ThinningPrescriptionsController.class);

    private final GetThinningPrescriptionQueryService queryService;
    private final FruitThinningPrescriptionCommandService commandService;
    private final ThinningPrescriptionIssuer issuer;
    private final ThinningPrescriptionOutlookService outlookService;
    private final String defaultProducerId;

    /**
     * Constructs the controller.
     *
     * @param queryService      read-only prescription query service
     * @param commandService    issues a pending prescription whose inputs became complete
     * @param issuer            tells what is still missing to issue a prescription
     * @param outlookService    builds the load before thinning, the sampling progress and the yield
     * @param defaultProducerId mock producer until IAM is in place
     */
    public ThinningPrescriptionsController(
            GetThinningPrescriptionQueryService queryService,
            FruitThinningPrescriptionCommandService commandService,
            ThinningPrescriptionIssuer issuer,
            ThinningPrescriptionOutlookService outlookService,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultProducerId
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.issuer = issuer;
        this.outlookService = outlookService;
        this.defaultProducerId = defaultProducerId;
    }

    /**
     * Lists the prescriptions of every active plot of the producer for a campaign.
     *
     * @param campaignYear optional campaign year; the current year by default
     * @return the prescriptions in any state, one per plot that was sampled
     */
    @GetMapping
    @Operation(
            summary = "List the thinning prescriptions of the producer",
            description = "Returns the prescription of every active plot of the producer for the campaign, in whatever state it is, with its blockers, the load before thinning and the yield. Plots never sampled have no prescription and are not listed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Prescriptions retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PrescriptionResource.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid campaign year")
    })
    public ResponseEntity<?> getThinningPrescriptions(
            @Parameter(description = "Agricultural campaign year", example = "2026")
            @RequestParam(name = "campaignYear", required = false) @Nullable Integer campaignYear
    ) {
        final CampaignYear targetCampaignYear;
        try {
            targetCampaignYear = campaignYear == null
                    ? new CampaignYear(Year.now(ZoneOffset.UTC).getValue())
                    : new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.<List<FruitThinningPrescriptionSnapshot>, ApplicationError>failure(
                            ApplicationError.validationError("request", exception.getMessage())),
                    this::toResources,
                    HttpStatus.OK
            );
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.handle(new GetThinningPrescriptionsQuery(defaultProducerId, targetCampaignYear)),
                this::toResources,
                HttpStatus.OK
        );
    }

    /**
     * Reads one prescription by its identifier.
     *
     * @param id prescription UUID
     * @return the prescription, or not found when it does not exist or its plot is no longer active
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a thinning prescription", description = "Reads one prescription by its identifier, in whatever state it is.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Prescription retrieved",
                    content = @Content(schema = @Schema(implementation = PrescriptionResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid prescription UUID"),
            @ApiResponse(responseCode = "404", description = "Prescription not found or its plot is no longer active")
    })
    public ResponseEntity<?> getThinningPrescription(
            @PathVariable @Parameter(description = "Unique prescription UUID", required = true) String id
    ) {
        final PrescriptionId prescriptionId;
        try {
            prescriptionId = new PrescriptionId(id);
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.<FruitThinningPrescriptionSnapshot, ApplicationError>failure(
                            ApplicationError.validationError("request", exception.getMessage())),
                    this::toResource,
                    HttpStatus.OK
            );
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.handle(new GetThinningPrescriptionByIdQuery(prescriptionId)),
                this::toResource,
                HttpStatus.OK
        );
    }

    private List<PrescriptionResource> toResources(List<FruitThinningPrescriptionSnapshot> snapshots) {
        return snapshots.stream().map(this::toResource).toList();
    }

    /** A pending prescription whose inputs became complete is issued first, like the per-plot read does. */
    private PrescriptionResource toResource(FruitThinningPrescriptionSnapshot snapshot) {
        FruitThinningPrescription prescription = FruitThinningPrescription.reconstitute(snapshot);
        if (snapshot.status() == PrescriptionStatus.SAMPLING_IN_PROGRESS) {
            try {
                var evaluated = commandService.handle(new EvaluateThinningPrescriptionCommand(
                        snapshot.plotId().plotId(), snapshot.campaignYear().value()));
                if (evaluated instanceof Result.Success<FruitThinningPrescription, ApplicationError> success) {
                    prescription = success.value();
                }
            } catch (RuntimeException exception) {
                LOG.warn("Thinning prescription {} could not be evaluated on read: {}",
                        snapshot.id().prescriptionId(), exception.getMessage());
            }
        }
        return PrescriptionResourceFromEntityAssembler.toResource(prescription.snapshot(),
                issuer.blockers(prescription), outlookService.outlookOf(prescription.snapshot()));
    }
}
