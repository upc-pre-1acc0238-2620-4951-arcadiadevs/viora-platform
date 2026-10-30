package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Y si no aclaro?": the load that would stay against the target. The platform does not estimate the effect of
 * not thinning on the next harvest, so the answer says so instead of inventing it.
 */
public final class IfNoneFactsAssembler extends PlanFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLAN_IF_NONE;
    }

    @Override
    protected AssembledFacts build(Plan plan, SortedMap<String, Object> facts) {
        double ratio = twoDecimals(plan.measuredLoad() / plan.target());
        facts.put("loadRatio", ratio);
        return new AssembledFacts(facts,
                text(questionId(), "text", plan.load(), ratio, plan.target(), plan.percent()),
                List.of(
                        card(shared("value.decimal", plan.load()), shared("card.unthinned.label"),
                                shared("card.target.pill", plan.target()), FactTone.PRIMARY),
                        card(shared("value.times", ratio), shared("card.times_target.label"),
                                shared("card.sampling.pill", plan.sampledTrees()), FactTone.HIGHLIGHT)),
                samplingSources(plan), List.of());
    }
}
