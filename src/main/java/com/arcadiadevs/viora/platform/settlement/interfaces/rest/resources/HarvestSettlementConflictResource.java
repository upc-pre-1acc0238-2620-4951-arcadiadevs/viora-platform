package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RFC 7807 problem detail returned when a campaign of a plot is settled twice, extended with the settlement that is
 * already in place.
 *
 * <p>It documents the body the API really sends: the standard {@code ProblemDetail} members plus the {@code code}
 * the error assembler always adds, plus the {@code existingSettlement} extension this change introduces. A plain
 * {@code ProblemDetail} remains the right type for every other failure of the endpoint.</p>
 *
 * <p>Documentation only: the body is built by the shared error assembler, never by hand.</p>
 *
 * @param type                problem type URI, derived from the error code
 * @param title               short human-readable summary of the status
 * @param status              HTTP status of the response
 * @param detail              localized explanation of this particular conflict
 * @param instance            URI of the request that failed
 * @param timestamp           instant the error was assembled
 * @param code                machine-readable error code
 * @param existingSettlement  settlement already registered for the campaign
 */
@Schema(description = "Problem detail of a campaign that is already settled, carrying the settlement in place")
public record HarvestSettlementConflictResource(
        @Schema(description = "Problem type URI", example = "https://api.viora.com/errors/harvestsettlement-conflict")
        String type,
        @Schema(description = "Summary of the status", example = "Conflict") String title,
        @Schema(description = "HTTP status of the response", example = "409") Integer status,
        @Schema(description = "Localized explanation of the conflict. This one has no placeholders.",
                example = "This campaign is already settled for the plot; a settlement cannot be overwritten.")
        String detail,
        @Schema(description = "URI of the failing request",
                example = "/api/v1/plots/3fa85f64-5717-4562-b3fc-2c963f66afa6/harvest-settlements")
        String instance,
        @Schema(description = "Instant the error was assembled", example = "2026-11-18T10:00:00Z") String timestamp,
        @Schema(description = "Machine-readable error code", example = "HARVESTSETTLEMENT_CONFLICT",
                allowableValues = {"HARVESTSETTLEMENT_CONFLICT"})
        String code,
        @Schema(description = "The settlement already registered for that campaign")
        ExistingSettlementResource existingSettlement) {
}
