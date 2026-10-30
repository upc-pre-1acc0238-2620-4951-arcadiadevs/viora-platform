package com.arcadiadevs.viora.platform.assistant.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.QuestionCatalogResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform.QuestionCatalogResourceFromEntityAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the closed questions the Viora assistant offers on each screen of the app.
 */
@RestController
@RequestMapping(value = "/api/v1/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assistant", description = "Endpoints of the Viora assistant: closed questions about a plot")
@NullMarked
public class AssistantQuestionController {

    private final QuestionCatalogQueryService queryService;
    private final CurrentUserProvider currentUserProvider;
    private final MessageSource messageSource;

    /**
     * Constructs the controller.
     *
     * @param queryService        evaluates the catalog of a screen
     * @param currentUserProvider resolves the producer asking
     * @param messageSource       the i18n message source for the labels and reasons
     */
    public AssistantQuestionController(QuestionCatalogQueryService queryService,
            CurrentUserProvider currentUserProvider, MessageSource messageSource) {
        this.queryService = queryService;
        this.currentUserProvider = currentUserProvider;
        this.messageSource = messageSource;
    }

    /**
     * Lists the questions of a screen, each enabled or disabled with its reason.
     *
     * @param screen screen key
     * @param plotId plot UUID, required by plot screens
     * @return the catalog or an RFC 7807 error
     */
    @GetMapping("/questions")
    @PreAuthorize("@plotAccess.isAccessible(#plotId)")
    @Operation(
            summary = "List the assistant questions of a screen",
            description = "Returns the closed questions of the screen in display order, with localized labels carrying the real figures of the plot (Accept-Language es/en). A question without enough data comes disabled with a localized disabledReason. Requires an active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questions of the screen",
                    content = @Content(schema = @Schema(implementation = QuestionCatalogResource.class))),
            @ApiResponse(responseCode = "400", description = "Unknown or missing screen, missing plotId for a plot screen, or malformed plot UUID"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan"),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive")
    })
    public ResponseEntity<?> listQuestions(
            @Parameter(description = "Screen key", example = "ALTERNATION") @RequestParam(required = false) @Nullable String screen,
            @Parameter(description = "Plot UUID; required by ALTERNATION and PLAN") @RequestParam(required = false) @Nullable String plotId
    ) {
        final GetQuestionCatalogQuery query;
        try {
            var screenKey = ScreenKey.fromValue(screen);
            query = new GetQuestionCatalogQuery(currentUserProvider.requireCurrentUserId(), screenKey,
                    plotId == null ? null : new PlotId(plotId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntityAssembler.toResponseEntityFromResult(
                    Result.<ScreenQuestionCatalog, ApplicationError>failure(
                            ApplicationError.validationError("request", exception.getMessage())),
                    this::toResource,
                    HttpStatus.OK);
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(queryService.handle(query), this::toResource,
                HttpStatus.OK);
    }

    private QuestionCatalogResource toResource(ScreenQuestionCatalog catalog) {
        return QuestionCatalogResourceFromEntityAssembler.toResource(catalog, messageSource,
                LocaleContextHolder.getLocale());
    }
}
