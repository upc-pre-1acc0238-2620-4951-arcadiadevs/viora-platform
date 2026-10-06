package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/** Immutable settlement voucher of a campaign. */
@Schema(description = "Settled campaign with its thinning balance and stabilization curve frozen at settlement")
public record HarvestSettlementResource(
        String id,
        String reportId,
        String plotId,
        Integer campaignYear,
        Double greenOlivesKg,
        Double blackOlivesKg,
        Double totalYieldKg,
        @Schema(nullable = true) Double commercialFruitsPerKg,
        @Schema(nullable = true) String notes,
        @Schema(allowableValues = {"SETTLED", "AUDITED"}) String status,
        Instant settledAt,
        ThinningBalanceResource thinningBalance,
        StabilizationCurveResource stabilization,
        @Schema(description = "Official receipt number of the settlement, numbered per producer and campaign",
                example = "VR-26-0001") String receiptNumber,
        @Schema(description = "Date the delivered olives were weighed", example = "2026-11-18") LocalDate weighedOn,
        @Schema(description = "Ticket number of the receiving mill", example = "MT-88213", nullable = true)
        String millTicketNumber,
        @Schema(description = "Commercial size grade of the settled caliber on the IOC size scale, derived from "
                + "commercialFruitsPerKg; the grade of 105.0 fruits per kilogram is 101/110. Absent when the "
                + "settlement carries no caliber.",
                example = "101/110", nullable = true)
        String commercialSizeGrade) {
}
