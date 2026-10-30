package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.List;

/**
 * Questions the assistant offers on a screen, in display order.
 *
 * @param screen    the screen
 * @param plotId    the plot the questions are about
 * @param questions questions in display order
 */
public record ScreenQuestionCatalog(ScreenKey screen, PlotId plotId, List<CatalogQuestion> questions) {

    public ScreenQuestionCatalog {
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
