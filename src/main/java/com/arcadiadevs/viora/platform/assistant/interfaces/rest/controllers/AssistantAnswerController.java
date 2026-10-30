package com.arcadiadevs.viora.platform.assistant.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.AssistantAnswerQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AnswerResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AskQuestionResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform.AnswerResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller answering the closed questions of the Viora assistant and reopening the stored answers.
 */
@RestController
@RequestMapping(value = "/api/v1/assistant/answers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assistant", description = "Endpoints of the Viora assistant: closed questions about a plot")
@NullMarked
public class AssistantAnswerController {

    private final AssistantAnswerCommandService commandService;
    private final AssistantAnswerQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Constructs the controller.
     *
     * @param commandService      answers the questions
     * @param queryService        reads the stored answers
     * @param currentUserProvider resolves the producer asking
     */
    public AssistantAnswerController(AssistantAnswerCommandService commandService,
            AssistantAnswerQueryService queryService, CurrentUserProvider currentUserProvider) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Answers a closed question about a plot, or about the whole field on HOME.
     *
     * @param resource the question
     * @return the answer (201 new, 200 cached or replayed) or an RFC 7807 error
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@plotAccess.isAccessible(#resource?.plotId())")
    @Operation(
            summary = "Ask the assistant a closed question",
            description = "Answers a question of the catalog with the real facts of the plot (HOME questions without "
                    + "plotId answer for every active plot of the producer, and are stored and cached apart): a "
                    + "localized text (Accept-Language es/en), exactly two fact cards, the sources and, while the "
                    + "thinning window is open, a link to record it. The text is worded by the configured model when "
                    + "enabled and validated against the facts; otherwise, or when it is rejected or fails, it comes "
                    + "from a deterministic template (model.fallback = true). The same clientRequestId returns the same "
                    + "answer (200); an answer with the same facts and language younger than the cache TTL is served "
                    + "again (200) unless forceRefresh is true. Requires an active plan. Limited per producer per "
                    + "hour (429 with Retry-After); replays are not counted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "New answer",
                    content = @Content(schema = @Schema(implementation = AnswerResource.class))),
            @ApiResponse(responseCode = "200", description = "Answer served from cache or replayed for the same "
                    + "clientRequestId", content = @Content(schema = @Schema(implementation = AnswerResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown questionId, missing plotId for a question "
                    + "of a plot screen, or malformed plotId or clientRequestId", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The question is disabled (detail = its disabledReason), "
                    + "or the clientRequestId was used for another question",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Too many questions by the producer (forceRefresh has "
                    + "its own, smaller limit); see Retry-After. A replay of a clientRequestId is never limited",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> ask(@RequestBody(required = false) @Nullable AskQuestionResource resource) {
        final AskQuestionCommand command;
        try {
            command = toCommand(resource);
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
        var result = commandService.handle(command);
        if (result instanceof Result.Success<AssistantAnswerCommandService.AnswerOutcome, ApplicationError> success) {
            var outcome = success.value();
            return new ResponseEntity<>(AnswerResourceFromEntityAssembler.toResource(outcome.view()),
                    outcome.created() ? HttpStatus.CREATED : HttpStatus.OK);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(result, outcome -> outcome, HttpStatus.OK);
    }

    /**
     * Lists the newest answers of the producer.
     *
     * @param limit maximum answers, 1 to 10; 10 when absent
     * @return newest first, or an RFC 7807 error
     */
    @GetMapping
    @Operation(summary = "List the recent answers",
            description = "Answers of the producer from the last 10 days, newest first, each with its current "
                    + "freshness. Requires an active plan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent answers, empty when there are none",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnswerResource.class)))),
            @ApiResponse(responseCode = "400", description = "limit outside 1..10",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "No active plan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> history(
            @Parameter(description = "Maximum answers, 1 to 10", example = "10")
            @RequestParam(required = false) @Nullable String limit) {
        final GetAnswerHistoryQuery query;
        try {
            query = new GetAnswerHistoryQuery(currentUserProvider.requireCurrentUserId(), parseLimit(limit));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query),
                views -> views.stream().map(AnswerResourceFromEntityAssembler::toResource).toList(), HttpStatus.OK);
    }

    /**
     * Finds the latest answer of a screen and plot.
     *
     * @param screen screen key
     * @param plotId plot UUID
     * @return 200 with the answer, 204 when there is none, or an RFC 7807 error
     */
    @GetMapping("/latest")
    @PreAuthorize("@plotAccess.isAccessible(#plotId)")
    @Operation(summary = "Get the latest answer of a screen",
            description = "Latest answer of the last 10 days to a question of the screen about the plot, so the "
                    + "entry opens on it until \"Otra pregunta\". Requires an active plan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest answer",
                    content = @Content(schema = @Schema(implementation = AnswerResource.class))),
            @ApiResponse(responseCode = "204", description = "No answer for the screen and plot"),
            @ApiResponse(responseCode = "400", description = "Unknown screen, missing plotId for a plot screen, "
                    + "or malformed plotId",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> latest(
            @Parameter(description = "Screen key", example = "PLAN") @RequestParam(required = false) @Nullable String screen,
            @Parameter(description = "Plot UUID") @RequestParam(required = false) @Nullable String plotId) {
        final GetLatestAnswerQuery query;
        try {
            var screenKey = ScreenKey.fromValue(screen);
            query = new GetLatestAnswerQuery(currentUserProvider.requireCurrentUserId(), screenKey,
                    plotId == null ? null : new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
        var result = queryService.handle(query);
        if (result instanceof Result.Success<?, ApplicationError> success
                && success.value() instanceof Optional<?> found && found.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                view -> AnswerResourceFromEntityAssembler.toResource(view.orElseThrow()), HttpStatus.OK);
    }

    /**
     * Reopens one answer of the producer.
     *
     * @param answerId answer UUID
     * @return the answer, or 404 when it is unknown, of another producer or older than 10 days
     */
    @GetMapping("/{answerId}")
    @Operation(summary = "Get an answer",
            description = "Full answer with its current freshness, to reopen it from the history. An answer of "
                    + "another producer or older than 10 days answers 404. Requires an active plan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answer",
                    content = @Content(schema = @Schema(implementation = AnswerResource.class))),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "No active plan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Unknown answer, of another producer or expired",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> get(@PathVariable String answerId) {
        final UUID id;
        try {
            id = UUID.fromString(answerId);
        } catch (IllegalArgumentException exception) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.notFound("assistant_answer", answerId));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(
                queryService.handle(new GetAnswerByIdQuery(currentUserProvider.requireCurrentUserId(), id)),
                AnswerResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }

    private AskQuestionCommand toCommand(@Nullable AskQuestionResource resource) {
        if (resource == null || resource.questionId() == null) {
            throw new IllegalArgumentException("assistant.question.invalid");
        }
        var questionId = new QuestionId(resource.questionId().trim());
        if (QuestionCatalog.screenOf(questionId).isEmpty()) {
            throw new IllegalArgumentException("assistant.question.invalid");
        }
        // Without plot, the command accepts only a question of a screen with field scope (HOME)
        var plotId = resource.plotId() == null ? null : new PlotId(resource.plotId());
        return new AskQuestionCommand(currentUserProvider.requireCurrentUserId(), questionId, plotId,
                parseClientRequestId(resource.clientRequestId()), Boolean.TRUE.equals(resource.forceRefresh()),
                LocaleContextHolder.getLocale());
    }

    private static UUID parseClientRequestId(@Nullable String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank()) {
            throw new IllegalArgumentException("assistant.client_request.invalid");
        }
        try {
            return UUID.fromString(clientRequestId.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("assistant.client_request.invalid", exception);
        }
    }

    private static int parseLimit(@Nullable String limit) {
        if (limit == null || limit.isBlank()) {
            return AssistantAnswer.HISTORY_SIZE;
        }
        try {
            return Integer.parseInt(limit.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("assistant.limit.invalid", exception);
        }
    }

    private static ResponseEntity<?> invalid(@Nullable String reason) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("request", reason == null ? "assistant.request.invalid" : reason));
    }
}
