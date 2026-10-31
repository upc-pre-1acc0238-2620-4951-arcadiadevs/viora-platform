package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo está este lote?": two sentences, the bearing index with its band and where the thinning of the campaign
 * stands (contract: "vecería 0,51 + 12 días", the days left in the open window). Either may be missing, not both.
 */
public final class PlotHowIsFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLOT_HOW_IS;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var plan = plot.plan();
        var state = window(data.today(), plan);
        boolean hasIndex = plot.bearingIndex() != null && plot.bearingCategory() != null;
        if (!hasIndex && state == Window.NONE) {
            return Optional.empty();
        }
        var sources = new ArrayList<CatalogText>();
        final CatalogText indexText;
        final FactCardTemplate indexCard;
        if (hasIndex) {
            double index = twoDecimals(plot.bearingIndex());
            var band = shared("band." + plot.bearingCategory());
            facts.put("bearingIndex", index);
            facts.put("bearingBand", plot.bearingCategory());
            indexText = text(questionId(), "index", index, band);
            indexCard = card(shared("value.decimal", index), shared("card.bbi.label"), band, FactTone.PRIMARY);
            if (!plot.yields().isEmpty()) {
                facts.put("harvestCampaigns", plot.yields().size());
                sources.add(shared("source.campaigns", plot.yields().size()));
            }
        } else {
            indexText = text(questionId(), "index.none");
            indexCard = card(shared("card.no_index.value"), shared("card.bbi.label"), shared("card.no_index.pill"),
                    FactTone.PRIMARY);
        }
        putWindow(data.today(), plan, state, facts);
        if (state == Window.SAMPLING) {
            facts.put("treesNeeded", plot.treesNeeded());
        }
        if (plan != null && plan.sampledTrees() > 0) {
            sources.add(plan.lastSampledOn() == null
                    ? shared("source.sampling_undated", plan.sampledTrees())
                    : shared("source.sampling", plan.sampledTrees(), TextDate.of(plan.lastSampledOn())));
        }
        return Optional.of(new AssembledFacts(facts,
                text(questionId(), "text", indexText, thinningText(data.today(), plan, state, plot.treesNeeded())),
                List.of(indexCard, windowCard(data.today(), plan, state)), sources, List.of()));
    }

    private CatalogText thinningText(LocalDate today, ThinningPlanData plan, Window state, int treesNeeded) {
        return switch (state) {
            case OPEN -> text(questionId(), "thinning.open", daysLeft(today, plan),
                    whole(plan.percentageToRemove()), TextDate.of(plan.windowClosesOn()));
            case BEFORE -> text(questionId(), "thinning.before", TextDate.of(plan.windowOpensOn()),
                    whole(plan.percentageToRemove()));
            case CLOSED -> text(questionId(), "thinning.closed", TextDate.of(plan.windowClosesOn()));
            case DONE -> text(questionId(), "thinning.done", TextDate.of(plan.executedOn()));
            case SAMPLING -> treesNeeded > 0
                    ? text(questionId(), "thinning.sampling", treesNeeded)
                    : text(questionId(), "thinning.unissued");
            case NONE -> text(questionId(), "thinning.none");
        };
    }
}
