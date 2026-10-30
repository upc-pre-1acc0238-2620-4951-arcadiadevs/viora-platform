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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuánto cosecharé este año?": the yield and caliber the thinning context projected with the load the confirmed
 * thinning left (US28). For the field it gives the range of the kilograms per hectare among the plots with an
 * estimate; it never adds them up, since a field total would need the planted area of each plot.
 */
public final class HomeHarvestEstimateFactsAssembler extends HomeFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.HOME_HARVEST_ESTIMATE;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData home, SortedMap<String, Object> facts) {
        var estimates = home.caliberEstimates();
        if (estimates.isEmpty()) {
            return Optional.empty();
        }
        facts.put("campaignYear", data.campaignYear());
        return home.singlePlot() ? plot(estimates.getFirst(), facts) : field(home, estimates, facts);
    }

    private Optional<AssembledFacts> plot(ThinningPlanData plan, SortedMap<String, Object> facts) {
        long fruitsPerKg = whole(plan.mostLikelyFruitsPerKg());
        facts.put("mostLikelyFruitsPerKg", fruitsPerKg);
        facts.put("commercialSizeGrade", plan.commercialSizeGrade());
        if (plan.executedOn() != null) {
            facts.put("thinningRecordedOn", plan.executedOn().toString());
        }
        var caliberCard = card(shared("value.integer", fruitsPerKg), shared("card.caliber.label"),
                shared("card.grade.pill", plan.commercialSizeGrade()), FactTone.HIGHLIGHT);
        final FactCardTemplate yieldCard;
        final CatalogText text;
        if (plan.estimatedKgPerHaAfterThinning() != null) {
            long kgPerHa = whole(plan.estimatedKgPerHaAfterThinning());
            facts.put("estimatedKgPerHa", kgPerHa);
            yieldCard = card(shared("value.integer", kgPerHa), shared("card.yield.label"),
                    shared("card.calibrating.pill"), FactTone.PRIMARY);
            text = text(questionId(), "text.yield", kgPerHa, fruitsPerKg, plan.commercialSizeGrade());
        } else {
            yieldCard = noYieldCard();
            text = text(questionId(), "text.no_yield", fruitsPerKg, plan.commercialSizeGrade());
        }
        var sources = new ArrayList<CatalogText>();
        if (plan.executedOn() != null) {
            sources.add(shared("source.thinning", TextDate.of(plan.executedOn())));
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(yieldCard, caliberCard), sources, List.of()));
    }

    private Optional<AssembledFacts> field(HomeData home, List<ThinningPlanData> estimates,
            SortedMap<String, Object> facts) {
        int active = home.activePlots();
        var yields = estimates.stream().map(ThinningPlanData::estimatedKgPerHaAfterThinning)
                .filter(kg -> kg != null)
                .map(AnswerTexts::whole)
                .sorted()
                .toList();
        facts.put("estimatedPlots", estimates.size());
        facts.put("yieldPlots", yields.size());
        final CatalogText text;
        final FactCardTemplate yieldCard;
        final int counted;
        if (yields.isEmpty()) {
            counted = estimates.size();
            text = text(questionId(), "text.field.no_yield", counted, active);
            yieldCard = noYieldCard();
        } else {
            counted = yields.size();
            long min = yields.getFirst();
            long max = yields.getLast();
            int without = active - counted;
            facts.put("minKgPerHa", min);
            facts.put("maxKgPerHa", max);
            facts.put("plotsWithoutYield", without);
            text = min == max
                    ? text(questionId(), "text.field.single", counted, active, min, max, without)
                    : text(questionId(), "text.field.range", counted, active, min, max, without);
            yieldCard = card(min == max ? shared("value.integer", min) : shared("value.integer_range", min, max),
                    shared("card.yield.label"), shared("card.calibrating.pill"), FactTone.PRIMARY);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(yieldCard,
                card(shared("value.integer", counted), shared("card.estimated_plots.label", counted),
                        shared("card.of_active.pill", active), FactTone.HIGHLIGHT)),
                List.of(shared("source.thinning.field")), List.of()));
    }

    private static FactCardTemplate noYieldCard() {
        return card(shared("card.no_yield.value"), shared("card.no_yield.label"), shared("card.no_yield.pill"),
                FactTone.PRIMARY);
    }
}
