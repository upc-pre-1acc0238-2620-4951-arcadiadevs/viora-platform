package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuándo es mejor aclarear?": the window of the prescription and where it stands today.
 */
public final class BestTimeFactsAssembler extends PlanFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLAN_BEST_TIME;
    }

    @Override
    protected AssembledFacts build(Plan plan, SortedMap<String, Object> facts) {
        var opens = TextDate.of(plan.opensOn());
        var closes = TextDate.of(plan.closesOn());
        var opensShort = TextDate.shortOf(plan.opensOn());
        var closesShort = TextDate.shortOf(plan.closesOn());
        final CatalogText text;
        final FactCardTemplate windowCard;
        switch (plan.state()) {
            case OPEN -> {
                text = text(questionId(), "text.open", opens, closes, plan.daysLeft());
                windowCard = card(shared("value.integer", plan.daysLeft()),
                        shared("card.days_left.label", plan.daysLeft()), shared("card.closes.pill", closesShort),
                        FactTone.PRIMARY);
            }
            case BEFORE -> {
                text = text(questionId(), "text.before", opens, closes, plan.daysUntilOpen());
                windowCard = card(shared("value.integer", plan.daysUntilOpen()),
                        shared("card.days_to_open.label", plan.daysUntilOpen()),
                        shared("card.opens.pill", opensShort), FactTone.PRIMARY);
            }
            case CLOSED -> {
                text = text(questionId(), "text.closed", closes);
                windowCard = card(shared("card.closed.value"), shared("card.window.label"),
                        shared("card.closed_on.pill", closesShort), FactTone.PRIMARY);
            }
            default -> {
                text = text(questionId(), "text.done", TextDate.of(plan.executedOn()), opens, closes);
                windowCard = card(shared("card.done.value"), shared("card.done.label"),
                        shared("card.done.pill", TextDate.shortOf(plan.executedOn())), FactTone.PRIMARY);
            }
        }
        var percentCard = card(shared("value.percent", plan.percent()), shared("card.percent_to_remove.label"),
                windowPill(plan), FactTone.HIGHLIGHT);
        return new AssembledFacts(facts, text, List.of(windowCard, percentCard), samplingSources(plan),
                registerAction(plan));
    }
}
