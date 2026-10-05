package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * REST response resource representing a thinning prescription, its intervention window and what is
 * still missing to issue it.
 *
 * @param id                   prescription UUID
 * @param plotId               plot UUID
 * @param campaignYear         agricultural campaign year
 * @param targetFruitsPerShoot sustainable target, in fruits per sampled shoot
 * @param percentageToRemove   recommended removal percentage
 * @param loadUnit             unit of every load of the thinning API: {@code FRUITS_PER_SHOOT}
 * @param status               prescription lifecycle status
 * @param fullBloomOn          observed full bloom date the window is counted from
 * @param windowOpensOn        first recommended intervention date
 * @param windowClosesOn       latest recommended intervention date
 * @param windowBasis          how the window was obtained: {@code FULL_BLOOM_PLUS_PROFILE_OFFSETS}
 * @param profileVersion       version of the technical profile that supplied the target and the window
 * @param profileStatus        approval status of that profile
 * @param isWindowOpen         whether the intervention window is currently open
 * @param blockers             inputs still missing to issue the prescription; empty once issued
 * @param issuedAt             timestamp when the prescription was issued
 */
@Schema(name = "PrescriptionResource", description = "Technical thinning prescription, its intervention window and the inputs still missing")
public record PrescriptionResource(
        @Schema(description = "Unique thinning prescription UUID", example = "7b2d5a39-c1f4-4b53-bca9-59eb88d440aa")
        String id,

        @Schema(description = "Unique plot UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String plotId,

        @Schema(description = "Agricultural campaign year", example = "2026")
        Integer campaignYear,

        @Schema(description = "Sustainable target, in fruits per sampled shoot; null until the prescription is issued", example = "0.4")
        Double targetFruitsPerShoot,

        @Schema(description = "Recommended percentage of fruits to remove; null until the prescription is issued", example = "28.25")
        Double percentageToRemove,

        @Schema(description = "Unit of every crop load of the thinning API", example = "FRUITS_PER_SHOOT")
        String loadUnit,

        @Schema(description = "Prescription lifecycle status", example = "PRESCRIBED")
        String status,

        @Schema(description = "Observed full bloom date of the campaign, if recorded", example = "2026-10-15")
        LocalDate fullBloomOn,

        @Schema(description = "First recommended thinning date", example = "2026-10-29")
        LocalDate windowOpensOn,

        @Schema(description = "Latest recommended thinning date", example = "2026-12-03")
        LocalDate windowClosesOn,

        @Schema(description = "How the window was obtained", example = "FULL_BLOOM_PLUS_PROFILE_OFFSETS")
        String windowBasis,

        @Schema(description = "Version of the technical profile the target and the window come from", example = "demo-1")
        String profileVersion,

        @Schema(description = "Approval status of that profile: AGRONOMIST_APPROVED, PROVISIONAL or SYNTHETIC_DEMO", example = "SYNTHETIC_DEMO")
        String profileStatus,

        @Schema(description = "Whether the thinning intervention window is currently open", example = "true")
        boolean isWindowOpen,

        @Schema(description = "Inputs still missing to issue the prescription: SAMPLING_NOT_REPRESENTATIVE, TARGET_NOT_CONFIGURED, FULL_BLOOM_MISSING")
        List<String> blockers,

        @Schema(description = "Timestamp at which the prescription was issued", example = "2026-10-16T12:00:00Z")
        Instant issuedAt
) {
}
