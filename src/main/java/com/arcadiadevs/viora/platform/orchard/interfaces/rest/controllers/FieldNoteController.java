package com.arcadiadevs.viora.platform.orchard.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.FieldNoteCommandService;
import com.arcadiadevs.viora.platform.orchard.application.queryservices.FieldNoteQueryService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.AttachFieldNotePhotoCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RecordFieldNoteCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.FieldNoteResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.RecordFieldNoteResource;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform.FieldNoteResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Field notes of the logbook (P75): what the producer did or saw in a plot, with an optional photo.
 */
@RestController
@RequestMapping(value = "/api/v1/plots/{plotId}/field-notes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Field Notes", description = "Free notes of the producer's logbook, one plot each, with an optional photo")
@PreAuthorize("@plotAccess.isAccessible(#plotId)")
public class FieldNoteController {

    private final FieldNoteCommandService commandService;
    private final FieldNoteQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * @param commandService      writes notes and photos
     * @param queryService        reads them
     * @param currentUserProvider resolves the producer of each request
     */
    public FieldNoteController(FieldNoteCommandService commandService, FieldNoteQueryService queryService,
            CurrentUserProvider currentUserProvider) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Write a field note",
            description = "Stores a note about an active plot. The phone generates the id, so sending the same note "
                    + "again after a lost connection answers 200 with the stored one instead of duplicating it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note stored",
                    content = @Content(schema = @Schema(implementation = FieldNoteResource.class))),
            @ApiResponse(responseCode = "200", description = "The same note was already stored; it is returned",
                    content = @Content(schema = @Schema(implementation = FieldNoteResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid id, type, text or a day in the future",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The plot belongs to another producer",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found or not active",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Another note already uses this id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> record(@PathVariable String plotId, @Valid @RequestBody RecordFieldNoteResource resource) {
        var command = new RecordFieldNoteCommand(resource.id(), plotId, currentUserProvider.requireCurrentUserId(),
                resource.type(), resource.text(), resource.observedOn());
        var result = commandService.handle(command);
        if (result instanceof Result.Success<FieldNoteCommandService.Recording, ApplicationError> success) {
            var recording = success.value();
            return new ResponseEntity<>(FieldNoteResourceAssembler.toResource(recording.note()),
                    recording.created() ? HttpStatus.CREATED : HttpStatus.OK);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(result, recording -> recording, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "List the field notes of a plot", description = "Newest first, by day and then by the "
            + "instant they were stored. Archived plots keep their notes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notes of the plot, empty when there are none",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FieldNoteResource.class)))),
            @ApiResponse(responseCode = "403", description = "The plot belongs to another producer",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> list(@PathVariable String plotId) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.findByPlot(plotId, currentUserProvider.requireCurrentUserId()),
                notes -> notes.stream().map(FieldNoteResourceAssembler::toResource).toList(),
                HttpStatus.OK);
    }

    @PutMapping(value = "/{noteId}/photo", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @Operation(summary = "Attach the photo of a field note",
            description = "Body: the JPEG or PNG bytes, up to 1 MiB. Replaces the previous photo, so it can be "
                    + "retried safely.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo stored; the note is returned",
                    content = @Content(schema = @Schema(implementation = FieldNoteResource.class))),
            @ApiResponse(responseCode = "400", description = "Empty, too large or not a JPEG/PNG image",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot or note not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> attachPhoto(@PathVariable String plotId, @PathVariable String noteId,
            @RequestBody byte[] content) {
        var command = new AttachFieldNotePhotoCommand(noteId, plotId, currentUserProvider.requireCurrentUserId(),
                content);
        return ResponseEntityAssembler.toResponseEntityFromResult(commandService.handle(command),
                FieldNoteResourceAssembler::toResource, HttpStatus.OK);
    }

    @GetMapping(value = "/{noteId}/photo", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @Operation(summary = "Download the photo of a field note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The photo bytes", content = {
                    @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "404", description = "Plot or note not found, or the note has no photo")
    })
    public ResponseEntity<?> photo(@PathVariable String plotId, @PathVariable String noteId) {
        var result = queryService.findPhoto(plotId, noteId, currentUserProvider.requireCurrentUserId());
        if (result instanceof Result.Success<FieldNotePhoto, ApplicationError> success) {
            byte[] bytes = success.value().content();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(success.value().mediaType()))
                    .contentLength(bytes.length)
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .body(bytes);
        }
        // An image client cannot read a JSON problem: the status says what happened
        var failure = (Result.Failure<FieldNotePhoto, ApplicationError>) result;
        return ResponseEntity.status(ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error())
                .getStatusCode()).build();
    }
}
