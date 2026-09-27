package com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects;

/**
 * Value object representing an agricultural campaign year (1980 <= year <= 2100).
 *
 * @param value the calendar year of the harvest campaign
 */
public record CampaignYear(Integer value) {

    public static final int MIN_YEAR = 1980;
    public static final int MAX_YEAR = 2100;

    /**
     * Compact constructor validating that the campaign year falls strictly within [1980, 2100].
     *
     * @throws IllegalArgumentException if value is null or outside the valid range
     */
    public CampaignYear {
        if (value == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (value < MIN_YEAR || value > MAX_YEAR) {
            throw new IllegalArgumentException("phenology.campaign_year.invalid");
        }
    }
}
