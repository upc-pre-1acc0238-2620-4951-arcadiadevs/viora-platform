package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * The settlement that is already registered for a campaign, published inside the 409 problem detail so the app can
 * show what it has to reconcile instead of asking again.
 *
 * <p>Documentation only: the object is built by the application layer and serialized by Spring's
 * {@code ProblemDetail}, never by hand.</p>
 *
 * @param campaignYear  campaign of the settlement already in place
 * @param totalYieldKg  green plus black kilograms of that settlement
 * @param receiptNumber receipt number of that settlement
 * @param weighedOn     date that settlement recorded as the weighing date; absent on settlements stored before the
 *                      receipt contract existed, until the one-off backfill reconstructs it
 */
@Schema(description = "Settlement already registered for the campaign that was being settled again")
public record ExistingSettlementResource(
        @Schema(description = "Settled campaign", example = "2026") Integer campaignYear,
        @Schema(description = "Green plus black olives of the existing settlement, kg", example = "7000.0")
        Double totalYieldKg,
        @Schema(description = "Receipt number already assigned to that settlement", example = "VR-26-0001")
        String receiptNumber,
        @Schema(description = "Weighing date already recorded, when the settlement carries one", example = "2026-11-18",
                nullable = true)
        LocalDate weighedOn) {
}
