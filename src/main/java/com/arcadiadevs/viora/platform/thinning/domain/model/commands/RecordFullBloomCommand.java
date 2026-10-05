package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

import java.time.LocalDate;

/**
 * Records the full bloom date observed on a plot in a campaign: the origin of the thinning window.
 *
 * @param plotId       plot UUID
 * @param campaignYear campaign the bloom belongs to
 * @param observedOn   day the plot reached full bloom
 */
public record RecordFullBloomCommand(String plotId, int campaignYear, LocalDate observedOn) {

    /**
     * Validates the command.
     */
    public RecordFullBloomCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("thinning.command.plot_id.null");
        }
        if (observedOn == null) {
            throw new IllegalArgumentException("thinning.full_bloom.null");
        }
    }
}
