package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Por qué el objetivo es 0,40?": the target load is the one the thinning profile of the variety sets, the load the
 * tree can sustain; the answer says whether an agronomist approved that profile, and where the thinning left the
 * load. Thinning never derives the target from the plot, so the answer never says it does.
 */
public final class ThinningWhyTargetFactsAssembler extends OnePlotFactsAssembler {

    /** Profile statuses with their own sentence; any other reads as "the profile of the variety". */
    private static final Set<String> PROFILE_STATUSES = Set.of("AGRONOMIST_APPROVED", "PROVISIONAL",
            "SYNTHETIC_DEMO");

    @Override
    public QuestionId questionId() {
        return QuestionId.THINNING_WHY_TARGET;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var outcome = home.outcome();
        if (outcome == null || outcome.targetLoad() == null) {
            return Optional.empty();
        }
        double target = twoDecimals(outcome.targetLoad());
        facts.put("targetLoad", target);
        var status = outcome.profileStatus() != null && PROFILE_STATUSES.contains(outcome.profileStatus())
                ? outcome.profileStatus() : null;
        if (status != null) {
            facts.put("profileStatus", status);
        }
        var profile = status == null
                ? text(questionId(), "profile.unknown")
                : text(questionId(), "profile." + status);
        var targetCard = card(shared("value.decimal", target), shared("card.target.label"),
                status == null ? shared("card.profile.pill") : shared("card.profile.pill." + status),
                FactTone.PRIMARY);
        final CatalogText text;
        final FactCardTemplate resultCard;
        if (outcome.preThinningLoad() != null && outcome.residualLoad() != null
                && outcome.actualRemovalPercentage() != null) {
            double pre = twoDecimals(outcome.preThinningLoad());
            double residual = twoDecimals(outcome.residualLoad());
            long removed = whole(outcome.actualRemovalPercentage());
            facts.put("preThinningLoad", pre);
            facts.put("residualLoad", residual);
            facts.put("removedPercent", removed);
            text = text(questionId(), "text", text(questionId(), "target", target), profile,
                    text(questionId(), "result", pre, removed, residual));
            resultCard = card(shared("value.decimal", residual), shared("card.residual_load.label"),
                    shared("card.removed.pill", removed), FactTone.HIGHLIGHT);
        } else {
            text = text(questionId(), "text.short", text(questionId(), "target", target), profile);
            resultCard = card(shared("card.done.value"), shared("card.done.label"),
                    shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(targetCard, resultCard),
                List.of(shared("source.thinning", TextDate.of(outcome.executedOn()))), List.of()));
    }
}
