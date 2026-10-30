package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * REST response resource representing an answer of the assistant.
 *
 * @param answerId    answer identifier
 * @param questionId  question answered
 * @param plotId      plot the question is about; null for a HOME answer about the whole field
 * @param createdAt   creation instant (ISO-8601, UTC)
 * @param dataVersion version of the facts the answer was built from
 * @param freshness   whether the data is still current
 * @param text        answer text
 * @param facts       exactly two fact cards
 * @param sources     what the answer is based on
 * @param actions     links to existing screens
 * @param model       what wrote the text
 */
@Schema(name = "AnswerResource", description = "Answer of the assistant to a closed question")
@NullMarked
public record AnswerResource(
        @Schema(description = "Answer identifier UUID") String answerId,
        @Schema(description = "Question identifier", example = "plan.why_percent") String questionId,
        @Schema(description = "Plot identifier UUID; null for a HOME answer about the producer's whole field",
                nullable = true) @Nullable String plotId,
        @Schema(description = "Creation instant, ISO-8601 UTC", example = "2026-10-28T12:00:00Z") String createdAt,
        @Schema(description = "Version of the facts", example = "sha256:3f2a…") String dataVersion,
        @Schema(description = "UP_TO_DATE while the data has not changed, STALE otherwise",
                allowableValues = {"UP_TO_DATE", "STALE"}) String freshness,
        @Schema(description = "Answer text, 2-3 sentences", example = "Tu carga está en 0,57 frutos por brote…")
        String text,
        @ArraySchema(minItems = 2, maxItems = 2, schema = @Schema(implementation = AnswerFactResource.class))
        List<AnswerFactResource> facts,
        @Schema(description = "What the answer is based on") List<AnswerSourceResource> sources,
        @Schema(description = "Links to existing screens; may be empty") List<AnswerActionResource> actions,
        @Schema(description = "What wrote the text") AnswerModelResource model
) {

    /**
     * A fact card.
     *
     * @param value big figure
     * @param label what the figure is
     * @param pill  short context
     * @param tone  PRIMARY or HIGHLIGHT
     */
    @Schema(name = "AnswerFactResource", description = "Fact card of an answer")
    public record AnswerFactResource(
            @Schema(example = "0,57") String value,
            @Schema(example = "frutos por brote · estimado") String label,
            @Schema(example = "objetivo 0,40") String pill,
            @Schema(allowableValues = {"PRIMARY", "HIGHLIGHT"}) String tone) {
    }

    /**
     * A source.
     *
     * @param label the source text
     */
    @Schema(name = "AnswerSourceResource", description = "What an answer is based on")
    public record AnswerSourceResource(@Schema(example = "Basado en 5 árboles, ronda del 29 de octubre") String label) {
    }

    /**
     * An action.
     *
     * @param type   DEEPLINK
     * @param label  button text
     * @param target route of the app
     */
    @Schema(name = "AnswerActionResource", description = "Link of an answer to an existing screen")
    public record AnswerActionResource(
            @Schema(example = "DEEPLINK") String type,
            @Schema(example = "Registrar aclareo") String label,
            @Schema(example = "thinning/execute?plotId=550e8400-e29b-41d4-a716-446655440001") String target) {
    }

    /**
     * What wrote the text.
     *
     * @param provider provider, {@code template} for the deterministic templates
     * @param name     model or template version
     * @param fallback whether the text came from a template
     */
    @Schema(name = "AnswerModelResource", description = "What wrote the text of an answer")
    public record AnswerModelResource(
            @Schema(example = "template") String provider,
            @Schema(example = "template-v1") String name,
            @Schema(example = "true") boolean fallback) {
    }
}
