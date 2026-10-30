package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.SortedMap;

/**
 * "¿Por qué 30 %?": the load the sampling measured against the target the tree can sustain.
 */
public final class WhyPercentFactsAssembler extends PlanFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLAN_WHY_PERCENT;
    }

    @Override
    protected AssembledFacts build(Plan plan, SortedMap<String, Object> facts) {
        return new AssembledFacts(facts,
                AnswerTexts.text(questionId(), "text", plan.load(), plan.target(), plan.percent()),
                List.of(loadCard(plan), removeCard(plan)),
                samplingSources(plan),
                registerAction(plan));
    }
}
