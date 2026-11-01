package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * REST response resource representing the daily summary of the home screen. Its cards, sources and model have the
 * shape of an answer's, so the app draws them the same way; it carries three cards instead of two.
 *
 * @param summaryId   summary identifier
 * @param questionId  always {@code home.daily_summary}
 * @param plotId      plot the summary is about; null for the producer's whole field
 * @param createdAt   when the summary was written
 * @param dataVersion fingerprint of the home figures it was written from
 * @param freshness   always UP_TO_DATE: a summary whose figures changed is written again
 * @param text        the sentences, in the language of the request
 * @param facts       exactly three cards: the thinning window, the maximum of the week and the open alerts
 * @param sources     what the summary is based on
 * @param model       who wrote the text
 */
@Schema(name = "AssistantDailySummaryResource", description = "The daily summary of the home screen")
@NullMarked
public record DailySummaryResource(
        @Schema(example = "0b6a5a3e-5c4f-4b8e-9a35-4b7e3e5f1a10") String summaryId,
        @Schema(example = "home.daily_summary") String questionId,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440001", nullable = true) @Nullable String plotId,
        @Schema(example = "2026-10-31T11:30:00Z") Instant createdAt,
        @Schema(example = "sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08") String dataVersion,
        @Schema(allowableValues = {"UP_TO_DATE"}) String freshness,
        @Schema(example = "Tu ventana de aclareo está abierta hasta el 10 de noviembre y el plan indica quitar el 30 % "
                + "de los frutos.") String text,
        @Schema(description = "Exactly three cards") List<AssistantAnswerResource.Fact> facts,
        List<AssistantAnswerResource.Source> sources,
        AssistantAnswerResource.Model model
) {
}
