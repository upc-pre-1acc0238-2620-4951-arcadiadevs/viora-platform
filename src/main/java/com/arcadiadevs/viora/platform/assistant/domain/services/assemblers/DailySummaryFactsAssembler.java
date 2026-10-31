package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.card;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.shared;

/**
 * The daily summary of Inicio ({@code home.daily_summary}, contract section 4.4): the answer of "¿Qué hago esta
 * semana?" (same facts, text and sources, for one plot or the whole field) with three cards of the same size: the
 * days of the thinning window, the maximum of the week and the open alerts. The window card reads the window the
 * week plan already chose: the open one that closes first, else the one opening this week; without either it says
 * there is none, without a number.
 */
public final class DailySummaryFactsAssembler implements FactsAssembler {

    private final HomeWeekPlanFactsAssembler weekPlan = new HomeWeekPlanFactsAssembler();

    @Override
    public QuestionId questionId() {
        return QuestionId.HOME_DAILY_SUMMARY;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        return weekPlan.assemble(data).map(week -> summary(data.today(), week));
    }

    private static AssembledFacts summary(LocalDate today, AssembledFacts week) {
        var facts = new TreeMap<>(week.facts());
        final FactCardTemplate windowCard;
        if (count(week, "plotsWindowOpen") > 0) {
            var closesOn = LocalDate.parse((String) week.facts().get("windowClosesOn"));
            // Today included, as the other window answers count it
            long daysLeft = ChronoUnit.DAYS.between(today, closesOn) + 1;
            facts.put("daysLeft", daysLeft);
            windowCard = card(shared("value.integer", daysLeft), shared("card.days_left.label", daysLeft),
                    shared("card.closes.pill", TextDate.shortOf(closesOn)), FactTone.PRIMARY);
        } else if (count(week, "plotsWindowOpening") > 0) {
            var opensOn = LocalDate.parse((String) week.facts().get("windowOpensOn"));
            long daysUntilOpen = ChronoUnit.DAYS.between(today, opensOn);
            facts.put("daysUntilOpen", daysUntilOpen);
            windowCard = card(shared("value.integer", daysUntilOpen), shared("card.days_to_open.label", daysUntilOpen),
                    shared("card.opens.pill", TextDate.shortOf(opensOn)), FactTone.PRIMARY);
        } else {
            windowCard = card(shared("card.no_window.value"), shared("card.window.label"),
                    shared("card.next_days.pill"), FactTone.PRIMARY);
        }
        // The week plan cards: the maximum of the week, then the open alerts
        return new AssembledFacts(facts, week.text(), List.of(windowCard, week.cards().get(0), week.cards().get(1)),
                week.sources(), List.of());
    }

    private static int count(AssembledFacts week, String fact) {
        return week.facts().get(fact) instanceof Integer value ? value : 0;
    }
}
