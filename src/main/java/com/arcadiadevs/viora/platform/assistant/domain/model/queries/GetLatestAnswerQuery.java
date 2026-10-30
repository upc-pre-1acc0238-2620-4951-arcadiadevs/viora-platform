package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

/**
 * Query finding the latest answer of a screen and plot, so the entry opens on it until "Otra pregunta".
 *
 * @param producerId producer asking
 * @param screen     screen the assistant is opened from
 * @param plotId     plot of the screen
 */
public record GetLatestAnswerQuery(String producerId, ScreenKey screen, PlotId plotId) {

    public GetLatestAnswerQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (screen == null) {
            throw new IllegalArgumentException("assistant.screen.invalid");
        }
        if (plotId == null) {
            throw new IllegalArgumentException("assistant.plot.required");
        }
    }
}
