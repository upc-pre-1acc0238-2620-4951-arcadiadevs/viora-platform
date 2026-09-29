package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing an agricultural campaign year bounded between 1980 and 2100.
 *
 * @param value the four-digit year
 */
public record CampaignYear(Integer value) {

    /**
     * Minimum allowed campaign year.
     */
    public static final int MIN_YEAR = 1980;

    /**
     * Maximum allowed campaign year.
     */
    public static final int MAX_YEAR = 2100;

    /**
     * Compact constructor validating year boundaries.
     */
    public CampaignYear {
        if (value == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
        if (value < MIN_YEAR || value > MAX_YEAR) {
            throw new IllegalArgumentException("thinning.campaign_year.invalid");
        }
    }
}
