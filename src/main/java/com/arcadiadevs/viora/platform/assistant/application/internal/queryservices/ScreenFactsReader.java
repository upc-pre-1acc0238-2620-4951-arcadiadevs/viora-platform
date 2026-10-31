package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

/**
 * Reads, through the ACL of the other contexts, the figures of one screen. Each screen has exactly one reader, and
 * its catalog and its answers are both evaluated against what it reads.
 */
public interface ScreenFactsReader {

    /**
     * The screen this reader reads the figures of.
     *
     * @return the screen
     */
    ScreenKey screen();

    /**
     * Reads the figures of the screen for a plot.
     *
     * @param plotId       the plot
     * @param campaignYear the current campaign year
     * @return the figures; those the plot does not have yet are null (or empty)
     */
    ScreenFacts read(PlotId plotId, int campaignYear);
}
