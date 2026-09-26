package com.arcadiadevs.viora.platform.orchard.domain.model.commands;

/**
 * Command to request the soft deletion and deactivation of an orchard plot.
 *
 * @param plotId     the identifier of the plot to be removed
 * @param producerId the identifier of the managing producer owning the plot
 * @param reason     the optional justification or reason for removal
 */
public record RemovePlotCommand(
        String plotId,
        String producerId,
        String reason
) {

    /**
     * Compact constructor enforcing non-null identifiers.
     */
    public RemovePlotCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }
}
