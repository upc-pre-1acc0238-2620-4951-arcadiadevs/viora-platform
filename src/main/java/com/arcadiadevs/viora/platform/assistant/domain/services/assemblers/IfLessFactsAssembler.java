package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;

import java.util.List;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Y si aclaro solo 20 %?": the load each percentage would leave, from the measured load. The only arithmetic is
 * {@code load x (1 - percent / 100)}, the same balance the thinning context uses; its results are facts.
 */
public final class IfLessFactsAssembler extends PlanFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLAN_IF_LESS;
    }

    @Override
    protected AssembledFacts build(Plan plan, SortedMap<String, Object> facts) {
        // Same scenario the catalog label offers
        int less = Math.max(0, plan.percent() - QuestionCatalog.LESS_THINNING_POINTS);
        double afterPlan = twoDecimals(plan.measuredLoad() * (1 - plan.percent() / 100.0));
        facts.put("lessPercent", less);
        facts.put("loadAfterPlan", afterPlan);
        var planCard = card(shared("value.decimal", afterPlan), shared("card.load_with.label", plan.percent()),
                shared("card.per_plan.pill"), FactTone.HIGHLIGHT);
        if (less == 0) {
            return new AssembledFacts(facts,
                    text(questionId(), "text.minimum", plan.percent(), plan.load(), plan.target()),
                    List.of(card(shared("value.decimal", plan.load()), shared("card.unthinned.label"),
                            shared("card.target.pill", plan.target()), FactTone.PRIMARY), planCard),
                    samplingSources(plan), List.of());
        }
        double afterLess = twoDecimals(plan.measuredLoad() * (1 - less / 100.0));
        facts.put("loadAfterLess", afterLess);
        return new AssembledFacts(facts,
                text(questionId(), "text", plan.percent(), afterPlan, plan.target(), less, afterLess),
                List.of(card(shared("value.decimal", afterLess), shared("card.load_with.label", less),
                        shared("card.target.pill", plan.target()), FactTone.PRIMARY), planCard),
                samplingSources(plan), List.of());
    }
}
