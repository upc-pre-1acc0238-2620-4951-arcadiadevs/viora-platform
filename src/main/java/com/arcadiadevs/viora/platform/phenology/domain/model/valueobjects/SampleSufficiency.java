package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Agronomic statistical sufficiency classification for historical harvest campaign series.
 *
 * <p>In accordance with biological specifications:
 * <ul>
 *   <li>A minimum of 3 consecutive agricultural campaigns is required to validate the formal
 *       Hoblyn Biennial Bearing Index ($BBI$).</li>
 *   <li>Series with fewer than 3 campaigns are classified as {@code INSUFFICIENT}.</li>
 * </ul>
 * </p>
 */
public enum SampleSufficiency {
    SUFFICIENT,
    INSUFFICIENT;

    public static final int MIN_REQUIRED_CAMPAIGNS = 3;

    /**
     * Determines sample sufficiency given the number of recorded harvest campaigns.
     *
     * @param campaignsCount the number of evaluated harvest campaigns
     * @return {@link #SUFFICIENT} if count &ge; 3; otherwise {@link #INSUFFICIENT}
     */
    public static SampleSufficiency of(int campaignsCount) {
        return campaignsCount >= MIN_REQUIRED_CAMPAIGNS ? SUFFICIENT : INSUFFICIENT;
    }

    /**
     * Checks if the sample size is considered sufficient.
     *
     * @return true if sufficient; false otherwise
     */
    public boolean isSufficient() {
        return this == SUFFICIENT;
    }
}
