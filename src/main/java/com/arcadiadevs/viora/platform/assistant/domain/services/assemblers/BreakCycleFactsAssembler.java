package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué hago para romper la vecería?": the index and what the thinning plan of the campaign prescribes. Without a
 * plan it only invites to sample; it never suggests a percentage of its own.
 */
public final class BreakCycleFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALTERNATION_BREAK_CYCLE;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        if (data.bearingIndex() == null || data.bearingCategory() == null) {
            return Optional.empty();
        }
        double index = twoDecimals(data.bearingIndex());
        int campaigns = data.historicalYields().size();
        var plan = data.plan();
        boolean issued = plan != null && plan.issued() && plan.percentageToRemove() != null;

        var facts = new TreeMap<String, Object>();
        facts.put("bearingIndex", index);
        facts.put("bearingBand", data.bearingCategory());
        facts.put("harvestCampaigns", campaigns);
        facts.put("prescriptionIssued", issued);
        var indexCard = card(shared("value.decimal", index), shared("card.bbi.label"),
                shared("band." + data.bearingCategory()), FactTone.PRIMARY);
        final FactCardTemplate planCard;
        final String variant;
        final Object[] args;
        if (issued) {
            int percent = (int) whole(plan.percentageToRemove());
            facts.put("percentToRemove", percent);
            planCard = card(shared("value.percent", percent), shared("card.percent_to_remove.label"),
                    shared("card.per_plan.pill"), FactTone.HIGHLIGHT);
            variant = "text.plan";
            args = new Object[] {index, percent};
        } else {
            planCard = card(shared("card.no_plan.value"), shared("card.no_plan.label"), shared("card.no_plan.pill"),
                    FactTone.HIGHLIGHT);
            variant = "text.no_plan";
            args = new Object[] {index};
        }
        return Optional.of(new AssembledFacts(facts, text(questionId(), variant, args), List.of(indexCard, planCard),
                List.of(shared("source.campaigns", campaigns)), List.of()));
    }
}
