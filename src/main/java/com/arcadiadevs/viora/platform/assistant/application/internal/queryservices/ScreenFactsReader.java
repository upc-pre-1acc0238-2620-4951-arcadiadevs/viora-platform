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

    /**
     * Reads the figures of the screen for a plot or, on a screen with field scope, for the producer's whole field.
     * A plot screen always gets its plot, so it reads that plot.
     *
     * @param producerId   the producer asking; its active plots are the field
     * @param plotId       the plot; null for the whole field
     * @param campaignYear the current campaign year
     * @return the figures; those the plot or the field do not have yet are null (or empty)
     */
    default ScreenFacts read(String producerId, PlotId plotId, int campaignYear) {
        return read(plotId, campaignYear);
    }
}
