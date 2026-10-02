package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

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
        StabilizationCurveResource stabilization) {
}
