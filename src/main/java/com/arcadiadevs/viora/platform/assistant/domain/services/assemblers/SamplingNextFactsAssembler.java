package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué hago ahora?": the next step after the sampling, by where the thinning stands: sample the trees still
 * missing, wait for the window, thin while it is open (with the "Registrar aclareo" action, as on PLAN), or nothing
 * once the thinning is recorded.
 */
public final class SamplingNextFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.SAMPLING_NEXT;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var plan = plot.plan();
        var today = data.today();
        var state = window(today, plan);
        if (state == Window.NONE) {
            return Optional.empty();
        }
        putWindow(today, plan, state, facts);
        facts.put("treesNeeded", plot.treesNeeded());
        List<ActionTemplate> actions = List.of();
        final CatalogText text = switch (state) {
            case SAMPLING -> plot.treesNeeded() > 0
                    ? text(questionId(), "text.sample", plot.treesNeeded(),
                            SamplingEnoughTreesFactsAssembler.MIN_REPRESENTATIVE_TREES)
                    : text(questionId(), "text.unissued");
            case BEFORE -> text(questionId(), "text.before", TextDate.of(plan.windowOpensOn()),
                    whole(plan.percentageToRemove()));
            case OPEN -> {
                actions = List.of(new ActionTemplate(ActionTemplate.DEEPLINK, shared("action.register_thinning"),
                        "thinning/execute?plotId=" + data.plotId().plotId()));
                yield text(questionId(), "text.open", daysLeft(today, plan), TextDate.of(plan.windowClosesOn()),
                        whole(plan.percentageToRemove()));
            }
            case CLOSED -> text(questionId(), "text.closed", TextDate.of(plan.windowClosesOn()));
            case DONE -> text(questionId(), "text.done", TextDate.of(plan.executedOn()));
            case NONE -> throw new IllegalStateException("unreachable");
        };
        return Optional.of(new AssembledFacts(facts, text, List.of(
                SamplingEnoughTreesFactsAssembler.treesCard(plan, plot.treesNeeded()),
                windowCard(today, plan, state)),
                List.of(SamplingEnoughTreesFactsAssembler.samplingSource(plan)), actions));
    }
}
