package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuánto cosecharé en 2027?": the yield and the caliber the thinning context projected with the load the
 * confirmed thinning left. It speaks of the harvest of the current campaign; there is no projection beyond it.
 */
public final class NextYearFactsAssembler implements FactsAssembler {

    private static final String ESTIMATED = "ESTIMATED";

    @Override
    public QuestionId questionId() {
        return QuestionId.ALTERNATION_NEXT_YEAR;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var plan = data.plan();
        if (plan == null || !ESTIMATED.equals(plan.caliberProjectionStatus()) || plan.mostLikelyFruitsPerKg() == null
                || plan.commercialSizeGrade() == null) {
            return Optional.empty();
        }
        long fruitsPerKg = whole(plan.mostLikelyFruitsPerKg());
        int campaigns = data.historicalYields().size();
        var facts = new TreeMap<String, Object>();
        facts.put("campaignYear", data.campaignYear());
        facts.put("harvestCampaigns", campaigns);
        facts.put("mostLikelyFruitsPerKg", fruitsPerKg);
        facts.put("commercialSizeGrade", plan.commercialSizeGrade());
        if (plan.executedOn() != null) {
            facts.put("thinningRecordedOn", plan.executedOn().toString());
        }
        var caliberCard = card(shared("value.integer", fruitsPerKg), shared("card.caliber.label"),
                shared("card.grade.pill", plan.commercialSizeGrade()), FactTone.HIGHLIGHT);
        final FactCardTemplate yieldCard;
        final String variant;
        final Object[] args;
        if (plan.estimatedKgPerHaAfterThinning() != null) {
            long kgPerHa = whole(plan.estimatedKgPerHaAfterThinning());
            facts.put("estimatedKgPerHa", kgPerHa);
            yieldCard = card(shared("value.integer", kgPerHa), shared("card.yield.label"),
                    shared("card.calibrating.pill"), FactTone.PRIMARY);
            variant = "text.yield";
            args = new Object[] {kgPerHa, fruitsPerKg, plan.commercialSizeGrade()};
        } else {
            yieldCard = card(shared("card.no_yield.value"), shared("card.no_yield.label"),
                    shared("card.no_yield.pill"), FactTone.PRIMARY);
            variant = "text.no_yield";
            args = new Object[] {fruitsPerKg, plan.commercialSizeGrade()};
        }
        var sources = new ArrayList<CatalogText>();
        if (plan.executedOn() != null) {
            sources.add(shared("source.thinning", TextDate.of(plan.executedOn())));
        }
        if (campaigns > 0) {
            sources.add(shared("source.campaigns", campaigns));
        }
        return Optional.of(new AssembledFacts(facts, text(questionId(), variant, args),
                List.of(yieldCard, caliberCard), sources, List.of()));
    }
}
