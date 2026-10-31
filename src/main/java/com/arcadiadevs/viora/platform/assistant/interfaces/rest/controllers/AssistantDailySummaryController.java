package com.arcadiadevs.viora.platform.assistant.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform.DailySummaryResourceFromEntityAssembler;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller serving the daily summary of the home screen.
 */
@RestController
@RequestMapping(value = "/api/v1/assistant/daily-summary", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assistant", description = "Endpoints of the Viora assistant: closed questions about a plot")
@NullMarked
public class AssistantDailySummaryController {

    private final DailySummaryCommandService commandService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Constructs the controller.
     *
     * @param commandService      prepares the summaries
     * @param currentUserProvider resolves the producer
     */
    public AssistantDailySummaryController(DailySummaryCommandService commandService,
                                           CurrentUserProvider currentUserProvider) {
        this.commandService = commandService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Serves the summary of the day of the producer's whole field, or of one plot.
     *
     * @param plotId plot UUID; absent for the whole field
     * @return the summary, or an RFC 7807 error
     */
    @GetMapping
    @PreAuthorize("@plotAccess.isAccessible(#plotId)")
    @Operation(
            summary = "Get the daily summary of the home screen",
            description = "The text of home.week_plan with exactly three fact cards of the same size: the days of the "
                    + "thinning window, the maximum of the week and the open alerts (Accept-Language es/en). Without "
                    + "plotId it is about every active plot of the producer. A job precomputes the field summary in "
                    + "Spanish once a day (06:30 in Lima); otherwise, or when its figures changed since, it is "
                    + "written on demand. A summary is served again the same day (in Lima) while its figures and "
                    + "language are the same, so freshness is always UP_TO_DATE. Summaries are not part of the "
                    + "answer history. Writing one on demand counts as a new answer of the producer (429 with "
                    + "Retry-After). Requires an active plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The summary of the day, stored or just written"),
            @ApiResponse(responseCode = "400", description = "Malformed plotId"),
            @ApiResponse(responseCode = "401", description = "No authenticated user"),
            @ApiResponse(responseCode = "403", description = "Plot of another producer, or no active plan"),
            @ApiResponse(responseCode = "404", description = "Plot does not exist or is inactive"),
            @ApiResponse(responseCode = "409", description = "Nothing to summarize: no active plots"),
            @ApiResponse(responseCode = "429", description = "Too many new answers by the producer; see Retry-After")
    })
    public ResponseEntity<?> dailySummary(
            @Parameter(description = "Plot UUID; absent for the producer's whole field")
            @RequestParam(required = false) @Nullable String plotId) {
        final PrepareDailySummaryCommand command;
        try {
            command = new PrepareDailySummaryCommand(currentUserProvider.requireCurrentUserId(),
                    plotId == null ? null : new PlotId(plotId), AnswerLanguage.of(LocaleContextHolder.getLocale()),
                    false);
        } catch (IllegalArgumentException exception) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("request", exception.getMessage()));
        }
        return ResponseEntityAssembler.toResponseEntityFromResult(commandService.handle(command),
                DailySummaryResourceFromEntityAssembler::toResource, HttpStatus.OK);
    }
}
