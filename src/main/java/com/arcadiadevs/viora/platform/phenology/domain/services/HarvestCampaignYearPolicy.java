package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;

import java.time.Clock;
import java.time.Year;

/**
 * Domain policy constraining which campaign years may be recorded as a completed harvest: a campaign can
 * only be recorded once the olive harvest of that year has actually happened.
 *
 * <p>{@link CampaignYear} remains a purely structural year guard shared across bounded contexts, so it keeps
 * its wide [1980, 2100] bounds. Recording a harvest applies the stricter agronomic range enforced by the
 * mobile design: {@value #MINIMUM_RECORDED_YEAR} or later, and never beyond the current year.
 *
 * <p>The current year is read from an injected {@link Clock} so the policy stays deterministic under test.
 */
public final class HarvestCampaignYearPolicy {

    /**
     * Oldest campaign year that may be recorded as a completed harvest.
     */
    public static final int MINIMUM_RECORDED_YEAR = 2000;

    private HarvestCampaignYearPolicy() {
    }

    /**
     * Validates a raw campaign year against the harvest recording range and builds the corresponding
     * {@link CampaignYear} value object.
     *
     * @param rawYear the unvalidated campaign year taken from the recording request
     * @param clock   the clock used to resolve the current year
     * @return a {@link CampaignYear} within [2000, current year]
     * @throws IllegalArgumentException if rawYear is null, earlier than {@value #MINIMUM_RECORDED_YEAR},
     *                                  or later than the current year
     */
    public static CampaignYear forHarvestRecording(Integer rawYear, Clock clock) {
        if (rawYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (rawYear < MINIMUM_RECORDED_YEAR) {
            throw new IllegalArgumentException("phenology.campaign_year.too_early");
        }
        if (rawYear > Year.now(clock).getValue()) {
            throw new IllegalArgumentException("phenology.campaign_year.future");
        }
        return new CampaignYear(rawYear);
    }
}
