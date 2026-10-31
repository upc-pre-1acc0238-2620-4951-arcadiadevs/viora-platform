package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué planifico para la próxima campaña?": only what Viora already knows. The alternation band of the bearing
 * index; whether the closed campaign was thinned, and on time, as Thinning recorded it; and the steps of the
 * thinning flow for the next campaign (full bloom, sampling, the window of the variety profile). It never projects
 * the next harvest: no context does.
 */
public final class CampaignPlanNextFactsAssembler extends OnePlotFactsAssembler {

    /** Bands with alternation, where the thinning by load is the measure of the plan. */
    private static final Set<String> ALTERNATING = Set.of("MODERATE_ALTERNATION", "SEVERE_ALTERNATION");

    /** Timeliness values with their own sentence; any other reads as a recorded thinning. */
    private static final Set<String> TIMELINESS = Set.of("OPTIMAL", "LATE");

    @Override
    public QuestionId questionId() {
        return QuestionId.CAMPAIGN_PLAN_NEXT;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var closed = home.closedCampaignYear();
        if (closed.isEmpty()) {
            return Optional.empty();
        }
        int closedYear = closed.get();
        int nextYear = closedYear + 1;
        String closedText = String.valueOf(closedYear);
        String nextText = String.valueOf(nextYear);
        int campaigns = plot.yields().size();
        facts.put("harvestCampaigns", campaigns);
        facts.put("lastCampaignYear", closedYear);
        facts.put("nextCampaignYear", nextYear);
        var sources = new ArrayList<CatalogText>(List.of(shared("source.campaigns", campaigns)));

        CatalogText bearing = null;
        FactCardTemplate secondCard = null;
        if (plot.bearingIndex() != null && plot.bearingCategory() != null) {
            double index = twoDecimals(plot.bearingIndex());
            String band = plot.bearingCategory();
            facts.put("bearingIndex", index);
            facts.put("bearingBand", band);
            bearing = text(questionId(), ALTERNATING.contains(band) ? "bearing.alternating" : "bearing.regular",
                    index, shared("band." + band));
            secondCard = card(shared("value.decimal", index), shared("card.bbi.label"), shared("band." + band),
                    FactTone.HIGHLIGHT);
        }

        var outcome = home.outcome();
        final CatalogText thinning;
        final CatalogText steps;
        facts.put("thinned", outcome != null);
        if (outcome != null) {
            facts.put("thinningRecordedOn", outcome.executedOn().toString());
            String timeliness = outcome.timeliness() != null && TIMELINESS.contains(outcome.timeliness())
                    ? outcome.timeliness() : null;
            if (timeliness != null) {
                facts.put("thinningTimeliness", timeliness);
            }
            thinning = text(questionId(), "thinning." + (timeliness == null ? "done" : timeliness), closedText,
                    TextDate.of(outcome.executedOn()));
            sources.add(shared("source.thinning", TextDate.of(outcome.executedOn())));
            if (secondCard == null) {
                secondCard = card(shared("card.done.value"), shared("card.closed_thinning.label", closedText),
                        shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
            }
        } else {
            thinning = text(questionId(), "thinning.none", closedText);
            if (secondCard == null) {
                secondCard = card(shared("card.not_thinned.value"), shared("card.closed_thinning.label", closedText),
                        shared("card.no_plan.pill"), FactTone.HIGHLIGHT);
            }
        }
        if (outcome != null && outcome.windowOpensDaysAfterBloom() != null
                && outcome.windowClosesDaysAfterBloom() != null) {
            int opens = outcome.windowOpensDaysAfterBloom();
            int closes = outcome.windowClosesDaysAfterBloom();
            facts.put("windowOpensDaysAfterBloom", opens);
            facts.put("windowClosesDaysAfterBloom", closes);
            steps = text(questionId(), "steps.window", nextText, opens, closes);
        } else {
            steps = text(questionId(), "steps", nextText);
        }

        var text = bearing == null
                ? text(questionId(), "text.short", thinning, steps)
                : text(questionId(), "text", bearing, thinning, steps);
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("card.date.value", nextText), shared("card.next_campaign.label"),
                        shared("card.after_bloom.pill"), FactTone.PRIMARY),
                secondCard), sources, List.of()));
    }
}
