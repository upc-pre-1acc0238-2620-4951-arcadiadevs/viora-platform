package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * REST response resource representing an answer of the assistant.
 *
 * @param answerId    answer identifier
 * @param questionId  question answered
 * @param plotId      plot the question was about
 * @param createdAt   when the answer was written
 * @param dataVersion fingerprint of the figures it was written from
 * @param freshness   UP_TO_DATE while the plot has the same data, STALE once it changed
 * @param text        the 2-3 sentences, in the language of the request that asked
 * @param facts       exactly two cards
 * @param sources     what the answer is based on
 * @param actions     buttons that open screens of the app
 * @param model       who wrote the text
 */
@Schema(name = "AssistantAnswerResource", description = "An answer of the assistant to a closed question")
@NullMarked
public record AssistantAnswerResource(
        @Schema(example = "0b6a5a3e-5c4f-4b8e-9a35-4b7e3e5f1a10") String answerId,
        @Schema(example = "plan.why_percent") String questionId,
        @Schema(example = "550e8400-e29b-41d4-a716-446655440001") String plotId,
        @Schema(example = "2026-10-30T20:00:00Z") Instant createdAt,
        @Schema(example = "sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08") String dataVersion,
        @Schema(allowableValues = {"UP_TO_DATE", "STALE"}) String freshness,
        @Schema(example = "Tu carga está en 0,57 frutos por brote y el árbol sostiene 0,40.") String text,
        @Schema(description = "Exactly two cards") List<Fact> facts,
        List<Source> sources,
        List<Action> actions,
        Model model
) {

    /**
     * A card under the text.
     *
     * @param value the figure shown big
     * @param label what the figure is
     * @param pill  the note under it
     * @param tone  PRIMARY (forest green) or HIGHLIGHT (light yellow)
     */
    @Schema(name = "AssistantAnswerFact")
    public record Fact(String value, String label, String pill,
                       @Schema(allowableValues = {"PRIMARY", "HIGHLIGHT"}) String tone) {
    }

    /**
     * What the answer is based on.
     *
     * @param label the text shown under the answer
     */
    @Schema(name = "AssistantAnswerSource")
    public record Source(String label) {
    }

    /**
     * A button that opens a screen of the app.
     *
     * @param type   DEEPLINK
     * @param label  button text
     * @param target route of the screen, for example thinning/execute?plotId=...
     */
    @Schema(name = "AssistantAnswerAction")
    public record Action(@Schema(allowableValues = {"DEEPLINK"}) String type, String label, String target) {
    }

    /**
     * Who wrote the text.
     *
     * @param provider TEMPLATE for the fixed sentences, or the model provider
     * @param name     model name; null for a template
     * @param fallback true when the text is the template because the model failed or its text was rejected
     */
    @Schema(name = "AssistantAnswerModel")
    public record Model(String provider, @Nullable String name, boolean fallback) {
    }
}
