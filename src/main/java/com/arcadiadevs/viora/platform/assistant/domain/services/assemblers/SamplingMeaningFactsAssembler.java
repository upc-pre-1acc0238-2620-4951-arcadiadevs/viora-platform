package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué significa este resultado?": the measured load against the load the tree can sustain, how many times it is
 * over it, and the percentage the prescription removes for that. Needs the issued prescription.
 */
public final class SamplingMeaningFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.SAMPLING_MEANING;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var plan = plot.plan();
        if (plan == null || !SamplingEnoughTreesFactsAssembler.hasLoads(plan)) {
            return Optional.empty();
        }
        double load = twoDecimals(plan.preThinningFruitsPerShoot());
        double target = twoDecimals(plan.targetFruitsPerShoot());
        if (target <= 0) {
            return Optional.empty();
        }
        // From the figures shown, in decimal, so 0,59 against 0,40 reads 1,48 as the producer would work it out
        double ratio = BigDecimal.valueOf(load).divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP)
                .doubleValue();
        long percent = whole(plan.percentageToRemove());
        facts.put("preThinningLoad", load);
        facts.put("targetLoad", target);
        facts.put("loadRatio", ratio);
        facts.put("percentToRemove", percent);
        facts.put("sampledTrees", plan.sampledTrees());
        var text = plan.preThinningFruitsPerShoot() > plan.targetFruitsPerShoot()
                ? text(questionId(), "text.over", load, target, ratio, percent)
                : text(questionId(), "text.within", load, target, percent);
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.decimal", load), shared("card.load.label"), shared("card.target.pill", target),
                        FactTone.PRIMARY),
                card(shared("value.percent", percent), shared("card.percent_to_remove.label"),
                        shared("card.per_plan.pill"), FactTone.HIGHLIGHT)),
                List.of(SamplingEnoughTreesFactsAssembler.samplingSource(plan)), List.of()));
    }
}
