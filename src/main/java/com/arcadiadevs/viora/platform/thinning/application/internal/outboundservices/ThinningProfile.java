package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import java.util.Set;

/**
 * Technical profile of a variety for the thinning prescription: the target load and the intervention
 * window, with who approved them and on what basis. Nothing about it is hardcoded in the platform.
 *
 * @param variety                    olive variety the profile applies to
 * @param targetFruitsPerShoot       sustainable target, in fruits per sampled shoot
 * @param windowOpensDaysAfterBloom  first thinning day, counted from the observed full bloom
 * @param windowClosesDaysAfterBloom last thinning day, counted from the observed full bloom
 * @param status                     {@code AGRONOMIST_APPROVED}, {@code PROVISIONAL} or {@code SYNTHETIC_DEMO}
 * @param version                    profile version, kept with every prescription it produced
 * @param source                     reference that justifies the values
 * @param approvedBy                 person or institution that approved the profile
 */
public record ThinningProfile(
        String variety,
        double targetFruitsPerShoot,
        int windowOpensDaysAfterBloom,
        int windowClosesDaysAfterBloom,
        String status,
        String version,
        String source,
        String approvedBy
) {

    /** Statuses a profile can have. {@code SYNTHETIC_DEMO} is only for demonstrations. */
    public static final Set<String> STATUSES = Set.of("AGRONOMIST_APPROVED", "PROVISIONAL", "SYNTHETIC_DEMO");

    /**
     * Validates that the profile is complete and coherent.
     */
    public ThinningProfile {
        if (!Double.isFinite(targetFruitsPerShoot) || targetFruitsPerShoot <= 0.0) {
            throw new IllegalArgumentException("thinning.profile.target.invalid");
        }
        if (windowOpensDaysAfterBloom < 0 || windowClosesDaysAfterBloom < windowOpensDaysAfterBloom) {
            throw new IllegalArgumentException("thinning.profile.window.invalid");
        }
        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException("thinning.profile.status.invalid");
        }
        if (isBlank(version) || isBlank(source) || isBlank(approvedBy)) {
            throw new IllegalArgumentException("thinning.profile.provenance.missing");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
