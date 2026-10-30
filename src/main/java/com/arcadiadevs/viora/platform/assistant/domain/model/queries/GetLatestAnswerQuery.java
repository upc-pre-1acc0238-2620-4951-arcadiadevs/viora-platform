package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

/**
 * Query for the last answer given on a screen about a plot, so the entry of the app reopens on it.
 *
 * @param producerId producer asking, whose plan must be active
 * @param screen     screen the assistant is opened from
 * @param plotId     plot of the screen
 */
public record GetLatestAnswerQuery(String producerId, ScreenKey screen, PlotId plotId) {

    /**
     * Compact constructor validating that nothing is missing.
     */
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
