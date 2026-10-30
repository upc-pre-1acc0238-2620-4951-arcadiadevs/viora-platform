package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo va mi campaña frente a la anterior?": the last registered harvest against the one before it, in total
 * kilograms as the producer registered them. For the field, only the plots that registered both campaigns are
 * summed, so the same plots are compared; the bearing index is per plot, so only a plot answer shows it.
 */
public final class HomeCampaignVsPreviousFactsAssembler extends HomeFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.HOME_CAMPAIGN_VS_PREVIOUS;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData home, SortedMap<String, Object> facts) {
        var found = home.campaignComparison();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var comparison = found.get();
        long lastKg = whole(comparison.lastKg());
        long previousKg = whole(comparison.previousKg());
        long difference = whole(Math.abs(comparison.lastKg() - comparison.previousKg()) / comparison.previousKg()
                * 100.0);
        String trend = difference == 0L ? "EQUAL" : comparison.lastKg() > comparison.previousKg() ? "ABOVE" : "BELOW";
        int compared = comparison.comparedPlots();
        boolean single = home.singlePlot();
        facts.put("lastCampaignYear", comparison.lastYear());
        facts.put("lastCampaignKg", lastKg);
        facts.put("previousCampaignYear", comparison.previousYear());
        facts.put("previousCampaignKg", previousKg);
        facts.put("differencePercent", difference);
        facts.put("trend", trend);
        facts.put("comparedPlots", compared);
        final CatalogText closing;
        final CatalogText pill;
        final CatalogText source;
        if (single) {
            var plot = home.plots().getFirst();
            int campaigns = plot.yields().size();
            facts.put("harvestCampaigns", campaigns);
            if (plot.bearingIndex() != null && plot.bearingCategory() != null) {
                double index = twoDecimals(plot.bearingIndex());
                facts.put("bearingIndex", index);
                facts.put("bearingBand", plot.bearingCategory());
                closing = text(questionId(), "bearing", index, shared("band." + plot.bearingCategory()));
            } else {
                closing = text(questionId(), "bearing.missing");
            }
            pill = shared("card.campaigns.pill", campaigns);
            source = shared("source.campaigns", campaigns);
        } else {
            int leftOut = home.activePlots() - compared;
            facts.put("plotsLeftOut", leftOut);
            closing = text(questionId(), "left_out", leftOut);
            pill = shared("card.compared_plots.pill", compared);
            source = shared("source.campaigns.field", compared);
        }
        String valueKey = switch (trend) {
            case "ABOVE" -> "value.plus_percent";
            case "BELOW" -> "value.minus_percent";
            default -> "value.percent";
        };
        String lastYear = String.valueOf(comparison.lastYear());
        String previousYear = String.valueOf(comparison.previousYear());
        return Optional.of(new AssembledFacts(facts,
                text(questionId(), "text." + trend.toLowerCase(Locale.ROOT) + (single ? "" : ".field"),
                        lastYear, lastKg, difference, previousYear, previousKg, closing, compared),
                List.of(
                        card(shared("value.kg", lastKg), shared("card.harvest.label", lastYear),
                                shared("card.previous_harvest.pill", previousYear, previousKg), FactTone.PRIMARY),
                        card(shared(valueKey, difference), shared("card.vs_previous.label"), pill,
                                FactTone.HIGHLIGHT)),
                List.of(source), List.of()));
    }
}
