package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

/**
 * Query requesting the questions the assistant offers on a screen.
 *
 * @param producerId producer asking, whose plan must be active
 * @param screen     screen the assistant is opened from
 * @param plotId     plot the questions are about; required by plot screens
 */
public record GetQuestionCatalogQuery(String producerId, ScreenKey screen, PlotId plotId) {

    /**
     * Compact constructor validating the screen and that plot screens name their plot.
     */
    public GetQuestionCatalogQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (screen == null) {
            throw new IllegalArgumentException("assistant.screen.invalid");
        }
        if (plotId == null && screen.requiresPlot()) {
            throw new IllegalArgumentException("assistant.plot.required");
        }
    }
}
