package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué dijo el último muestreo?": the sampling of the campaign as the thinning context sums it up, the trees and the
 * day of the latest round; with the prescription issued, the measured load against the target and the percentage
 * it leads to, otherwise the trees still missing. The thinning context exposes the rounds summed up, never one by
 * one, so the load is that of the whole sampling.
 */
public final class PlotLastSamplingFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLOT_LAST_SAMPLING;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var plan = plot.plan();
        if (plan == null || plan.sampledTrees() <= 0) {
            return Optional.empty();
        }
        int trees = plan.sampledTrees();
        facts.put("sampledTrees", trees);
        final CatalogText latest;
        final CatalogText treesPill;
        final CatalogText source;
        if (plan.lastSampledOn() == null) {
            latest = text(questionId(), "latest.undated");
            treesPill = shared("card.undated.pill");
            source = shared("source.sampling_undated", trees);
        } else {
            facts.put("lastSampledOn", plan.lastSampledOn().toString());
            latest = text(questionId(), "latest", TextDate.of(plan.lastSampledOn()));
            treesPill = shared("card.latest_round.pill", TextDate.shortOf(plan.lastSampledOn()));
            source = shared("source.sampling", trees, TextDate.of(plan.lastSampledOn()));
        }
        var treesCard = card(shared("value.integer", trees), shared("card.sampled_trees.label", trees), treesPill,
                FactTone.PRIMARY);
        boolean issued = plan.issued() && plan.percentageToRemove() != null
                && plan.preThinningFruitsPerShoot() != null && plan.targetFruitsPerShoot() != null;
        facts.put("prescriptionIssued", issued);
        final CatalogText text;
        final FactCardTemplate secondCard;
        if (issued) {
            double load = twoDecimals(plan.preThinningFruitsPerShoot());
            double target = twoDecimals(plan.targetFruitsPerShoot());
            long percent = whole(plan.percentageToRemove());
            facts.put("preThinningLoad", load);
            facts.put("targetLoad", target);
            facts.put("percentToRemove", percent);
            text = text(questionId(), "text.issued", trees, latest, load, target, percent);
            secondCard = card(shared("value.decimal", load), shared("card.load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else if (plot.treesNeeded() > 0) {
            facts.put("treesNeeded", plot.treesNeeded());
            text = text(questionId(), "text.halfway", trees, latest, plot.treesNeeded());
            secondCard = card(shared("value.integer", plot.treesNeeded()),
                    shared("card.trees_needed.label", plot.treesNeeded()), shared("card.for_plan.pill"),
                    FactTone.HIGHLIGHT);
        } else {
            text = text(questionId(), "text.pending", trees, latest);
            secondCard = card(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.sampling.pill", trees), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(treesCard, secondCard), List.of(source),
                List.of()));
    }
}
