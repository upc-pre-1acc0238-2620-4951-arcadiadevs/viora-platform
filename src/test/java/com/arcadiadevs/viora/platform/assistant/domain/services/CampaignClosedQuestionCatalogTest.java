package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.harvests;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withYields;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: CAMPAIGN_CLOSED availability")
class CampaignClosedQuestionCatalogTest {

    private static final CatalogText CALIBER_UNAVAILABLE =
            CatalogText.of("assistant.reason.caliber_history_unavailable");

    @Test
    @DisplayName("CAMPAIGN_CLOSED is a one-plot screen with the prefix campaign.")
    void screen() {
        assertThat(ScreenKey.fromValue("campaign_closed")).isEqualTo(ScreenKey.CAMPAIGN_CLOSED);
        assertThat(ScreenKey.CAMPAIGN_CLOSED.requiresPlot()).isTrue();
        assertThat(ScreenKey.CAMPAIGN_CLOSED.readsOnePlot()).isTrue();
        assertThat(ScreenKey.CAMPAIGN_CLOSED.questionPrefix()).isEqualTo("campaign.");
        assertThat(QuestionCatalog.screenOf(QuestionId.CAMPAIGN_PLAN_NEXT)).contains(ScreenKey.CAMPAIGN_CLOSED);
    }

    @Test
    @DisplayName("Closed campaign with earlier ones: contract order, the years in the labels, caliber always disabled")
    void closedWithEarlierCampaigns() {
        var questions = campaignClosed(harvests(2023, 8000.0, 9000.0, 12400.0));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "campaign.vs_previous", "campaign.caliber_change", "campaign.plan_next");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("history", "fruit", "calendar");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true);
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.campaign.vs_previous.label", "2025"));
        assertThat(questions.get(1).disabledReason()).isEqualTo(CALIBER_UNAVAILABLE);
        assertThat(questions.get(2).label())
                .isEqualTo(CatalogText.of("assistant.question.campaign.plan_next.label", "2026"));
    }

    @Test
    @DisplayName("One harvest: no comparison yet; no harvest: every question waits for it, caliber keeps its reason")
    void missingHarvests() {
        var one = campaignClosed(harvests(2025, 12400.0));
        assertThat(one).extracting(CatalogQuestion::enabled).containsExactly(false, false, true);
        assertThat(one.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.campaigns_missing"));

        var none = QuestionCatalog.questionsFor(ScreenKey.CAMPAIGN_CLOSED,
                PlotFacts.ofOnePlot(2026, null, new HomeData(false, List.of(bare()), null)));
        assertThat(none).noneMatch(CatalogQuestion::enabled);
        assertThat(none).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.harvest_missing"), CALIBER_UNAVAILABLE,
                CatalogText.of("assistant.reason.harvest_missing"));
        assertThat(none.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.campaign.vs_previous.label_without_year"));
        assertThat(none.get(2).label())
                .isEqualTo(CatalogText.of("assistant.question.campaign.plan_next.label_without_year"));
    }

    private static List<CatalogQuestion> campaignClosed(SortedMap<Integer, Double> yields) {
        return QuestionCatalog.questionsFor(ScreenKey.CAMPAIGN_CLOSED, PlotFacts.ofOnePlot(2026, null,
                new HomeData(false, List.of(withYields(yields, null, null)), null)));
    }
}
