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

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Son suficientes árboles?": the trees sampled this campaign against the minimum Thinning asks for a
 * representative sampling and, once the prescription is issued, the load against the target (contract: "5/5 y 0,59
 * frutos por brote vs objetivo 0,40; mínimo 5 árboles").
 */
public final class SamplingEnoughTreesFactsAssembler extends OnePlotFactsAssembler {

    /**
     * Distinct trees Thinning needs for a representative sampling ({@code SamplingCoverageEvaluator
     * .MIN_REPRESENTATIVE_TREES}); a test keeps both equal. Thinning only exposes the trees still missing, so the
     * minimum is shown from here.
     */
    static final int MIN_REPRESENTATIVE_TREES = 5;

    @Override
    public QuestionId questionId() {
        return QuestionId.SAMPLING_ENOUGH_TREES;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var plan = plot.plan();
        if (plan == null || plan.sampledTrees() <= 0) {
            return Optional.empty();
        }
        int trees = plan.sampledTrees();
        int needed = plot.treesNeeded();
        facts.put("sampledTrees", trees);
        facts.put("minimumTrees", MIN_REPRESENTATIVE_TREES);
        facts.put("treesNeeded", needed);
        facts.put("representative", needed == 0);
        final CatalogText text;
        final FactCardTemplate loadCard;
        if (needed > 0) {
            text = text(questionId(), "text.short", trees, MIN_REPRESENTATIVE_TREES, needed);
            loadCard = card(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.sampling.pill", trees), FactTone.HIGHLIGHT);
        } else if (hasLoads(plan)) {
            double load = twoDecimals(plan.preThinningFruitsPerShoot());
            double target = twoDecimals(plan.targetFruitsPerShoot());
            facts.put("preThinningLoad", load);
            facts.put("targetLoad", target);
            text = text(questionId(), "text.enough", trees, MIN_REPRESENTATIVE_TREES, load, target);
            loadCard = card(shared("value.decimal", load), shared("card.load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else {
            text = text(questionId(), "text.enough.unissued", trees, MIN_REPRESENTATIVE_TREES);
            loadCard = card(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.sampling.pill", trees), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(treesCard(plan, needed), loadCard),
                List.of(samplingSource(plan)), List.of()));
    }

    /** "5/5 · árboles muestreados · muestra representativa" or "… · faltan 2". */
    static FactCardTemplate treesCard(ThinningPlanData plan, int needed) {
        return card(shared("value.fraction", plan.sampledTrees(), MIN_REPRESENTATIVE_TREES),
                shared("card.sampled_trees.label", plan.sampledTrees()),
                needed == 0 ? shared("card.representative.pill") : shared("card.trees_missing.pill", needed),
                FactTone.PRIMARY);
    }

    /** "Basado en 5 árboles, ronda del 29 de octubre". */
    static CatalogText samplingSource(ThinningPlanData plan) {
        return plan.lastSampledOn() == null
                ? shared("source.sampling_undated", plan.sampledTrees())
                : shared("source.sampling", plan.sampledTrees(), TextDate.of(plan.lastSampledOn()));
    }

    /** Whether the prescription is issued with the loads it comes from. */
    static boolean hasLoads(ThinningPlanData plan) {
        return plan.issued() && plan.percentageToRemove() != null && plan.preThinningFruitsPerShoot() != null
                && plan.targetFruitsPerShoot() != null;
    }
}
