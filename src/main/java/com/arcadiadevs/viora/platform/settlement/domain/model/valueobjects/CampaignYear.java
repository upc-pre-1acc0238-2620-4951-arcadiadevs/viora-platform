package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Agricultural campaign settled by Settlement: from 2000 to 2100 (own range, not the Thinning one).
 *
 * @param value campaign year
 */
public record CampaignYear(Integer value) {

    public static final int MIN_YEAR = 2000;
    public static final int MAX_YEAR = 2100;

    public CampaignYear {
        if (value == null) {
            throw new IllegalArgumentException("settlement.campaign_year.null");
        }
        if (value < MIN_YEAR || value > MAX_YEAR) {
            throw new IllegalArgumentException("settlement.campaign_year.invalid");
        }
    }
}
