package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;

import java.time.Year;
import java.time.ZoneOffset;

/**
 * Domain query representing a request for fruit thinning milestone events.
 *
 * @param actorId      producer or actor identifier
 * @param campaignYear agricultural campaign year
 * @param plotId       optional plot identifier filter
 */
public record GetThinningEventsQuery(
        String actorId,
        CampaignYear campaignYear,
        PlotId plotId
) {

    /**
     * Compact constructor validating required actor identifier and defaulting campaign year.
     */
    public GetThinningEventsQuery {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("thinning.actor.null_or_empty");
        }
        if (campaignYear == null) {
            campaignYear = new CampaignYear(Year.now(ZoneOffset.UTC).getValue());
        }
    }

    /**
     * Convenience constructor without plot filter.
     *
     * @param actorId      producer or actor identifier
     * @param campaignYear agricultural campaign year
     */
    public GetThinningEventsQuery(String actorId, CampaignYear campaignYear) {
        this(actorId, campaignYear, null);
    }
}
