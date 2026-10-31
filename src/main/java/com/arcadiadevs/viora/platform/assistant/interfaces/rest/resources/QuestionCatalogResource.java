package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * REST response resource representing the questions the assistant offers on a screen.
 *
 * @param screen    screen key
 * @param plotId    plot the questions are about; null for the producer's whole field (HOME without plotId)
 * @param questions questions in display order
 */
@Schema(name = "QuestionCatalogResource", description = "Questions of the assistant for a screen, in display order")
@NullMarked
public record QuestionCatalogResource(
        @Schema(description = "Screen key", example = "ALTERNATION", allowableValues = {"ALTERNATION", "PLAN"})
        String screen,

        @Schema(description = "Plot identifier UUID; null for HOME about the producer's whole field",
                example = "550e8400-e29b-41d4-a716-446655440001", nullable = true)
        @Nullable String plotId,

        @Schema(description = "Questions in display order")
        List<QuestionResource> questions
) {
}
