package com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * REST response resource representing one closed question the assistant offers.
 *
 * @param questionId     question identifier
 * @param label          localized question text with the real figures of the plot
 * @param icon           key of the Viora icon catalog
 * @param enabled        whether the question can be answered now
 * @param disabledReason localized reason why it cannot; null when enabled
 */
@Schema(name = "QuestionResource", description = "A closed question of the assistant catalog")
@NullMarked
public record QuestionResource(
        @Schema(description = "Question identifier", example = "alternation.why_index")
        String questionId,

        @Schema(description = "Localized label with the real figures", example = "¿Por qué mi índice es 0,51?")
        String label,

        @Schema(description = "Icon key of the Viora icon catalog", example = "calendar")
        String icon,

        @Schema(description = "Whether there is enough data to answer it", example = "true")
        boolean enabled,

        @Schema(description = "Localized reason why it is disabled; null when enabled",
                example = "Faltan datos de calibre · registra un muestreo", nullable = true)
        @Nullable String disabledReason
) {
}
