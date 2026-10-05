package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * REST response resource representing a thinning prescription and its intervention window.
 *
 * @param id                   prescription UUID
 * @param plotId               plot UUID
 * @param campaignYear        agricultural campaign year
 * @param targetFruitsPerShoot recommended sustainable fruit density
 * @param percentageToRemove   recommended removal percentage
 * @param status               prescription lifecycle status
 * @param windowClosesOn       latest recommended intervention date
 * @param isWindowOpen         whether the intervention window is currently open
 * @param issuedAt             timestamp when the prescription was issued
 */
@Schema(name = "PrescriptionResource", description = "Technical thinning prescription and fenological intervention window")
public record PrescriptionResource(
        @Schema(description = "Unique thinning prescription UUID", example = "7b2d5a39-c1f4-4b53-bca9-59eb88d440aa")
        String id,

        @Schema(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "Agricultural campaign year", example = "2026")
        Integer campaignYear,

        @Schema(description = "Recommended sustainable fruits per shoot", example = "8.5")
        Double targetFruitsPerShoot,

        @Schema(description = "Recommended percentage of fruits to remove", example = "28.25")
        Double percentageToRemove,

        @Schema(description = "Prescription lifecycle status", example = "PRESCRIBED")
        String status,

        @Schema(description = "Latest recommended thinning date", example = "2026-11-20")
        LocalDate windowClosesOn,

        @Schema(description = "Whether the thinning intervention window is currently open", example = "true")
        boolean isWindowOpen,

        @Schema(description = "Timestamp at which the prescription was issued", example = "2026-10-16T12:00:00Z")
        Instant issuedAt
) {
}
