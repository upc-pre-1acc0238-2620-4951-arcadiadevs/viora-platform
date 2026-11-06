package com.arcadiadevs.viora.platform.territory.domain.model.commands;

/**
 * Command a member sends to give one of their plots a sector: a zone of the list or, when it is missing, the name they
 * write.
 *
 * @param producerId   the authenticated producer
 * @param plotId       the plot
 * @param sectorZoneId the zone chosen from the list; null when writing a name
 * @param name         the sector written by the producer; null when choosing a zone
 */
public record AssignPlotSectorCommand(String producerId, String plotId, String sectorZoneId, String name) {

    /**
     * Compact constructor enforcing the required fields.
     */
    public AssignPlotSectorCommand {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (plotId == null) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
    }
}
