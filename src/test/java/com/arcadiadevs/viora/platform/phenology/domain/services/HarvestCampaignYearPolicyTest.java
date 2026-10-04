package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HarvestCampaignYearPolicy Domain Service Tests")
class HarvestCampaignYearPolicyTest {

    // Fixed at 2026 so every boundary below is deterministic
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Should reject a null campaign year with phenology.campaign_year.null")
    void shouldRejectNullCampaignYear() {
        assertThatThrownBy(() -> HarvestCampaignYearPolicy.forHarvestRecording(null, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.campaign_year.null");
    }

    @Test
    @DisplayName("Should reject campaign years before 2000 with phenology.campaign_year.too_early")
    void shouldRejectCampaignYearsBeforeTwoThousand() {
        assertThatThrownBy(() -> HarvestCampaignYearPolicy.forHarvestRecording(1999, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.campaign_year.too_early");
    }

    @Test
    @DisplayName("Should accept the minimum recordable campaign year 2000")
    void shouldAcceptMinimumRecordableCampaignYear() {
        var campaignYear = HarvestCampaignYearPolicy.forHarvestRecording(2000, clock);

        assertThat(campaignYear.value()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Should accept the current campaign year as recorded by the fixed clock")
    void shouldAcceptCurrentCampaignYear() {
        var campaignYear = HarvestCampaignYearPolicy.forHarvestRecording(2026, clock);

        assertThat(campaignYear.value()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Should reject campaign years after the current year with phenology.campaign_year.future")
    void shouldRejectCampaignYearsAfterCurrentYear() {
        assertThatThrownBy(() -> HarvestCampaignYearPolicy.forHarvestRecording(2027, clock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.campaign_year.future");
    }

    @Test
    @DisplayName("Should reject an out-of-window year before checking the structural CampaignYear guard")
    void shouldRejectOutOfWindowYearWithinStructuralGuard() {
        // 1999 satisfies the shared [1980, 2100] CampaignYear guard, so only the policy can reject it
        assertThat(new CampaignYear(1999).value()).isEqualTo(1999);
        assertThatThrownBy(() -> HarvestCampaignYearPolicy.forHarvestRecording(1999, clock))
                .hasMessage("phenology.campaign_year.too_early");
    }
}
