package com.arcadiadevs.viora.platform.telemetry.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.resources.MessageResource;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.IoTDeviceCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.IoTDeviceQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.DeactivateIoTDeviceCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetIoTDevicesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.DeviceId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.CalibrateIoTDeviceResource;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.IoTDeviceResource;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.resources.RegisterIoTDeviceResource;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.CalibrateIoTDeviceCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.IoTDeviceResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.telemetry.interfaces.rest.transform.RegisterIoTDeviceCommandFromResourceAssembler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for IoT sensor nodes and edaphic probe management.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/iot-devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "IoT Devices", description = "Endpoints for virtual IoT sensor nodes and edaphic probe management")
@NullMarked
public class IoTDeviceController {

    private final IoTDeviceCommandService ioTDeviceCommandService;
    private final IoTDeviceQueryService ioTDeviceQueryService;
    private final IoTDeviceRepository ioTDeviceRepository;

    /**
     * Constructs the controller injecting required service and repository ports.
     *
     * @param ioTDeviceCommandService the command service orchestrating device mutations
     * @param ioTDeviceQueryService   the query service retrieving device representations
     * @param ioTDeviceRepository     the domain device repository port
     */
    public IoTDeviceController(
            IoTDeviceCommandService ioTDeviceCommandService,
            IoTDeviceQueryService ioTDeviceQueryService,
            IoTDeviceRepository ioTDeviceRepository
    ) {
        this.ioTDeviceCommandService = ioTDeviceCommandService;
        this.ioTDeviceQueryService = ioTDeviceQueryService;
        this.ioTDeviceRepository = ioTDeviceRepository;
    }

    /**
     * Registers and binds a new virtual IoT sensor node to the specified orchard plot.
     *
     * @param plotId   the unique plot UUID
     * @param resource the registration payload
     * @return 201 Created with the registered IoTDeviceResource, or ProblemDetail on failure
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Register and bind virtual IoT device to plot",
            description = "Registers a new virtual sensor node (microclimate station or edaphic soil probe) bound to the specified plot."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "IoT device successfully registered and bound to plot",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload or geometric boundary validation error"),
            @ApiResponse(responseCode = "409", description = "A device with the same name already exists in the plot")
    })
    public ResponseEntity<?> registerIoTDevice(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Valid @RequestBody RegisterIoTDeviceResource resource
    ) {
        var command = RegisterIoTDeviceCommandFromResourceAssembler.toCommandFromResource(plotId, resource);
        var result = ioTDeviceCommandService.handle(command)
                .flatMap(deviceId -> ioTDeviceRepository.findById(new DeviceId(deviceId))
                        .<Result<IoTDevice, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("IoTDevice", deviceId))));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                IoTDeviceResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED
        );
    }

    /**
     * Lists all virtual IoT devices associated with the specified plot.
     *
     * @param plotId the unique plot UUID
     * @return 200 OK with list of IoTDeviceResource
     */
    @GetMapping
    @Operation(
            summary = "List all IoT devices bound to a plot",
            description = "Retrieves all virtual IoT sensor nodes associated with the specified plot identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "IoT devices successfully retrieved",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid plot UUID format")
    })
    public ResponseEntity<List<IoTDeviceResource>> listIoTDevices(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId
    ) {
        var query = new GetIoTDevicesByPlotIdQuery(plotId);
        var devices = ioTDeviceQueryService.handle(query);
        var resources = IoTDeviceResourceFromEntityAssembler.toResourceList(devices);
        return ResponseEntity.ok(resources);
    }

    /**
     * Calibrates an existing IoT sensor device offset multiplier, depth, and soil texture.
     * Enforces optimistic locking revision if provided via the If-Match header.
     *
     * @param plotId   the unique plot UUID
     * @param deviceId the unique device UUID
     * @param ifMatch  optional If-Match header containing the expected revision
     * @param resource the calibration payload
     * @return 200 OK with the updated IoTDeviceResource, or ProblemDetail on error
     */
    @PutMapping(value = "/{deviceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Calibrate sensor offset and soil factor",
            description = "Updates the empirical calibration multiplier, soil texture, and depth for an active IoT device."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "IoT device successfully calibrated",
                    content = @Content(schema = @Schema(implementation = IoTDeviceResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid payload or calibration parameters"),
            @ApiResponse(responseCode = "404", description = "IoT device not found"),
            @ApiResponse(responseCode = "409", description = "Device is not associated with the specified plot"),
            @ApiResponse(responseCode = "412", description = "Precondition Failed: If-Match revision mismatch"),
            @ApiResponse(responseCode = "422", description = "Device is not in ACTIVE status")
    })
    public ResponseEntity<?> calibrateIoTDevice(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Unique device UUID", example = "7b2d5a39-c1f4-4b53-bca9-59eb88d440aa")
            @PathVariable String deviceId,
            @Parameter(description = "Optimistic locking revision", example = "\"0\"")
            @RequestHeader(value = "If-Match", required = false) @Nullable String ifMatch,
            @Valid @RequestBody CalibrateIoTDeviceResource resource
    ) {
        Long expectedRevision = null;
        if (ifMatch != null && !ifMatch.isBlank()) {
            try {
                expectedRevision = Long.parseLong(ifMatch.replace("\"", "").trim());
            } catch (NumberFormatException ex) {
                var error = ApplicationError.validationError("If-Match", "device.revision.invalid");
                return ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
            }
        }

        var command = CalibrateIoTDeviceCommandFromResourceAssembler.toCommandFromResource(
                plotId, deviceId, expectedRevision, resource
        );

        var result = ioTDeviceCommandService.handle(command)
                .flatMap(id -> ioTDeviceRepository.findById(new DeviceId(id))
                        .<Result<IoTDevice, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("IoTDevice", id))));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                IoTDeviceResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }

    /**
     * Unlinks and logically deactivates an IoT sensor device from an active plot inventory.
     * Enforces optimistic locking revision if provided via the If-Match header.
     *
     * @param plotId   the unique plot UUID
     * @param deviceId the unique device UUID
     * @param ifMatch  optional If-Match header containing the expected revision
     * @return 200 OK with MessageResource on success, or ProblemDetail on error
     */
    @DeleteMapping("/{deviceId}")
    @Operation(
            summary = "Unlink and deactivate IoT sensor device",
            description = "Logically unlinks and deactivates an IoT sensor device from the active plot inventory, preserving historical telemetry series."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "IoT device unlinked successfully",
                    content = @Content(schema = @Schema(implementation = MessageResource.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or If-Match revision header"),
            @ApiResponse(responseCode = "404", description = "IoT device not found"),
            @ApiResponse(responseCode = "409", description = "Device is not associated with the specified plot or is already unlinked"),
            @ApiResponse(responseCode = "412", description = "Precondition Failed: If-Match revision mismatch")
    })
    public ResponseEntity<?> deactivateIoTDevice(
            @Parameter(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable String plotId,
            @Parameter(description = "Unique device UUID", example = "7b2d5a39-c1f4-4b53-bca9-59eb88d440aa")
            @PathVariable String deviceId,
            @Parameter(description = "Optimistic locking revision", example = "\"0\"")
            @RequestHeader(value = "If-Match", required = false) @Nullable String ifMatch
    ) {
        Long expectedRevision = null;
        if (ifMatch != null && !ifMatch.isBlank()) {
            try {
                expectedRevision = Long.parseLong(ifMatch.replace("\"", "").trim());
            } catch (NumberFormatException ex) {
                var error = ApplicationError.validationError("If-Match", "device.revision.invalid");
                return ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
            }
        }

        var command = new DeactivateIoTDeviceCommand(plotId, deviceId, expectedRevision);
        var result = ioTDeviceCommandService.handle(command)
                .map(unlinkedId -> new MessageResource("IoT device unlinked successfully"));

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                message -> message,
                HttpStatus.OK
        );
    }
}
