package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the intake of one campaign of the manager's license: projected if it is in progress, settled if it
 * is past.
 *
 * @param campaignYear the campaign
 * @param managerId    the signed-in manager
 */
public record GetIntakeForecastByCampaignYearAndManagerIdQuery(int campaignYear, ManagerId managerId) {

    /**
     * Compact constructor validating the fields.
     *
     * @param campaignYear the campaign
     * @param managerId    the signed-in manager
     */
    public GetIntakeForecastByCampaignYearAndManagerIdQuery {
        if (campaignYear < 2000 || campaignYear > 2100) {
            throw new IllegalArgumentException("territory.intake.campaign_year.invalid");
        }
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from a raw manager id.
     *
     * @param campaignYear the campaign
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetIntakeForecastByCampaignYearAndManagerIdQuery(int campaignYear, String rawManagerId) {
        this(campaignYear, new ManagerId(rawManagerId));
    }
}
