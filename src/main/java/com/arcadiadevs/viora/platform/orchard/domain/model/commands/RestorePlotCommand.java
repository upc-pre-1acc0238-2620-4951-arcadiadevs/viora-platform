package com.arcadiadevs.viora.platform.orchard.domain.model.commands;

/**
 * Command to bring an archived orchard plot back to the active inventory.
 *
 * @param plotId     the archived plot identifier
 * @param producerId the managing producer identifier
 */
public record RestorePlotCommand(
        String plotId,
        String producerId
) {

    /**
     * Compact constructor enforcing non-null constraints.
     */
    public RestorePlotCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }
}
