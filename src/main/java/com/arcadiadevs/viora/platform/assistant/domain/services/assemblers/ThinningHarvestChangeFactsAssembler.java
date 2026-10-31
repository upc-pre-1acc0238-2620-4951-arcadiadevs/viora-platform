package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo cambia mi cosecha?": the yield Thinning estimates with the load the thinning left against the one with the
 * load before it, and where that load stands against the target. Without a caliber model the yield follows the
 * number of fruits, so the estimate without thinning is a ceiling and the text says so. The contract mockup adds
 * "2027 más estable"; no context projects the next campaign, so the answer never says it
 * (docs/asistente-decisiones.md, Pantalla Aclareo registrado).
 */
public final class ThinningHarvestChangeFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.THINNING_HARVEST_CHANGE;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var outcome = home.outcome();
        if (outcome == null || outcome.kgPerHaAfterThinning() == null) {
            return Optional.empty();
        }
        long after = whole(outcome.kgPerHaAfterThinning());
        boolean ceiling = ThinningOutcomeData.FRUIT_COUNT_ONLY.equals(outcome.yieldBasis());
        facts.put("estimatedKgPerHa", after);
        facts.put("harvestCampaigns", outcome.harvestCampaigns());
        if (outcome.yieldBasis() != null) {
            facts.put("yieldBasis", outcome.yieldBasis());
        }
        final CatalogText yield;
        if (outcome.kgPerHaWithoutThinning() == null) {
            yield = text(questionId(), "yield.only", after);
        } else {
            long without = whole(outcome.kgPerHaWithoutThinning());
            facts.put("kgPerHaWithoutThinning", without);
            yield = ceiling
                    ? text(questionId(), "yield.ceiling", after, without)
                    : text(questionId(), "yield.compared", after, without);
        }
        var sentences = new ArrayList<Object>(List.of(yield));
        final FactCardTemplate loadCard;
        if (outcome.residualLoad() != null && outcome.targetLoad() != null && outcome.loadState() != null) {
            double residual = twoDecimals(outcome.residualLoad());
            double target = twoDecimals(outcome.targetLoad());
            facts.put("residualLoad", residual);
            facts.put("targetLoad", target);
            facts.put("loadState", outcome.loadState());
            sentences.add(text(questionId(), "load." + outcome.loadState(), residual, target));
            loadCard = card(shared("value.decimal", residual), shared("card.residual_load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else {
            loadCard = card(shared("card.date.value", TextDate.shortOf(outcome.executedOn())), shared("card.done.label"),
                    shared("card.window.done"), FactTone.HIGHLIGHT);
        }
        var text = sentences.size() == 2
                ? text(questionId(), "text", sentences.toArray())
                : text(questionId(), "text.single", sentences.toArray());
        var sources = new ArrayList<CatalogText>();
        sources.add(shared("source.thinning", TextDate.of(outcome.executedOn())));
        if (outcome.harvestCampaigns() > 0) {
            sources.add(shared("source.campaigns", outcome.harvestCampaigns()));
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.integer", after), shared("card.yield.label"),
                        ceiling ? shared("card.fruit_count.pill") : shared("card.calibrating.pill"), FactTone.PRIMARY),
                loadCard), sources, List.of()));
    }
}
