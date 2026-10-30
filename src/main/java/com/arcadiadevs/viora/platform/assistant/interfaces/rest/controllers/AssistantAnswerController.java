package com.arcadiadevs.viora.platform.assistant.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AskQuestionCommandService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.AssistantAnswerQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AskQuestionResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform.AssistantAnswerResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the answers of the Viora assistant: ask a closed question, reopen an answer, and list the
 * latest ones. The assistant only reads; an answer never changes the data of the plot.
 */
@RestController
@RequestMapping(value = "/api/v1/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assistant", description = "Endpoints of the Viora assistant: closed questions about a plot")
@NullMarked
public class AssistantAnswerController {

    private final AskQuestionCommandService commandService;
    private final AssistantAnswerQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Constructs the controller.
     *
     * @param commandService      answers a question
     * @param queryService        reads the saved answers
     * @param currentUserProvider resolves the producer asking
     */
    public AssistantAnswerController(AskQuestionCommandService commandService,
            AssistantAnswerQueryService queryService, CurrentUserProvider currentUserProvider) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Answers a closed question about a plot, in the language of the request.
     *
     * @param resource the question
     * @return the answer or an RFC 7807 error
     */
    @PostMapping(value = "/answers", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@plotAccess.isAccessible(#resource.plotId())")
    @Operation(
            summary = "Ask the assistant a closed question",
            description = "Answers a question of the catalog with the real figures of the plot (Accept-Language es/en). "
                    + "The same clientRequestId always gets the same answer; the data of the plot being unchanged "
                    + "reuses the saved answer unless forceRefresh is true. Requires an active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answer"),
            @ApiResponse(responseCode = "400", description = "Unknown question or screen, or a malformed id"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan"),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive"),
            @ApiResponse(responseCode = "409", description = "The question has not enough data to be answered")
    })
    public ResponseEntity<?> ask(@RequestBody AskQuestionResource resource) {
        final AskQuestionCommand command;
        try {
            command = new AskQuestionCommand(
                    currentUserProvider.requireCurrentUserId(),
                    new QuestionId(resource.questionId()),
                    resource.plotId() == null ? null : new PlotId(resource.plotId()),
                    new ClientRequestId(resource.clientRequestId()),
                    Boolean.TRUE.equals(resource.forceRefresh()),
                    AnswerLanguage.of(LocaleContextHolder.getLocale()));
        } catch (IllegalArgumentException exception) {
            return invalid(exception);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(commandService.handle(command),
                AssistantAnswerResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }

    /**
     * Lists the latest answers that have not expired, each with whether its data is still current.
     *
     * @param limit how many at most, 1 to 10
     * @return the answers, newest first
     */
    @GetMapping("/answers")
    @Operation(
            summary = "List the latest answers",
            description = "The latest answers of the producer from the last ten days, newest first, each marked "
                    + "UP_TO_DATE or STALE. Requires an active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answers"),
            @ApiResponse(responseCode = "400", description = "limit out of 1..10"),
            @ApiResponse(responseCode = "403", description = "No active plan")
    })
    public ResponseEntity<?> listAnswers(
            @Parameter(description = "How many answers at most", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        final GetAnswerHistoryQuery query;
        try {
            query = new GetAnswerHistoryQuery(currentUserProvider.requireCurrentUserId(), limit);
        } catch (IllegalArgumentException exception) {
            return invalid(exception);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query),
                AssistantAnswerResourceFromEntityAssembler::toHistoryResource, HttpStatus.OK);
    }

    /**
     * Returns the last answer given on a screen about a plot, so the entry of the app reopens on it.
     *
     * @param screen screen key
     * @param plotId plot UUID
     * @return the answer, or 404 when none was given
     */
    @GetMapping("/answers/latest")
    @PreAuthorize("@plotAccess.isAccessible(#plotId)")
    @Operation(
            summary = "Get the last answer of a screen",
            description = "The last answer given on the screen about the plot, from the last ten days. Requires an "
                    + "active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answer"),
            @ApiResponse(responseCode = "400", description = "Unknown or missing screen or plot"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan"),
            @ApiResponse(responseCode = "404", description = "No answer was given on that screen")
    })
    public ResponseEntity<?> latestAnswer(
            @Parameter(description = "Screen key", example = "PLAN") @RequestParam(required = false) @Nullable String screen,
            @Parameter(description = "Plot UUID") @RequestParam(required = false) @Nullable String plotId
    ) {
        final GetLatestAnswerQuery query;
        try {
            query = new GetLatestAnswerQuery(currentUserProvider.requireCurrentUserId(), ScreenKey.fromValue(screen),
                    plotId == null ? null : new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return invalid(exception);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query),
                AssistantAnswerResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }

    /**
     * Returns one saved answer, to reopen it from the history.
     *
     * @param answerId answer UUID
     * @return the answer, or 404 when it does not exist, is another producer's or expired
     */
    @GetMapping("/answers/{answerId}")
    @Operation(
            summary = "Get a saved answer",
            description = "One answer of the producer from the last ten days, with its freshness. Requires an active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answer"),
            @ApiResponse(responseCode = "400", description = "Malformed answer UUID"),
            @ApiResponse(responseCode = "403", description = "No active plan"),
            @ApiResponse(responseCode = "404", description = "No such answer")
    })
    public ResponseEntity<?> getAnswer(@PathVariable String answerId) {
        final GetAnswerByIdQuery query;
        try {
            query = new GetAnswerByIdQuery(currentUserProvider.requireCurrentUserId(), new AnswerId(answerId));
        } catch (IllegalArgumentException exception) {
            return invalid(exception);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query),
                AssistantAnswerResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }

    private static ResponseEntity<?> invalid(IllegalArgumentException exception) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("request", exception.getMessage()));
    }
}
