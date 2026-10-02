package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;

import java.time.Year;
import java.time.ZoneOffset;

/**
 * Query requesting the thinning prescription associated with an active orchard plot and campaign.
 *
 * @param plotId       target plot identifier
 * @param campaignYear agricultural campaign year; defaults to current UTC year when omitted
 * @param statusFilter optional lifecycle status filter
 */
public record GetActiveThinningPrescriptionQuery(
        PlotId plotId,
        CampaignYear campaignYear,
        PrescriptionStatus statusFilter
) {

    /**
     * Compact constructor validating the required plot identifier and defaulting the campaign year.
     */
    public GetActiveThinningPrescriptionQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.query.plot_id.null");
        }
        if (campaignYear == null) {
            campaignYear = new CampaignYear(Year.now(ZoneOffset.UTC).getValue());
        }
    }

    /**
     * Creates a query for the current campaign without a status filter.
     *
     * @param plotId target plot identifier
     */
    public GetActiveThinningPrescriptionQuery(PlotId plotId) {
        this(plotId, null, null);
    }

    /**
     * Creates a query for a campaign without a status filter.
     *
     * @param plotId target plot identifier
     * @param campaignYear agricultural campaign year
     */
    public GetActiveThinningPrescriptionQuery(PlotId plotId, CampaignYear campaignYear) {
        this(plotId, campaignYear, null);
    }
}
