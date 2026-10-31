package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;

/**
 * The daily summary of the home screen ({@code home.daily_summary}): the answer of "¿Qué hago esta semana?" (same
 * figures, text and source, for one plot or the whole field) with three cards of the same size: the days of the
 * thinning window, the maximum of the week and the open alerts. The window card reads the window the week plan already
 * chose: the open one that closes first, else the one opening this week; without either it says there is none,
 * without a number.
 */
public final class DailySummaryTexts {

    private DailySummaryTexts() {
    }

    /**
     * Writes the summary of the day.
     *
     * @param facts the figures of the plot or the field
     * @return the summary, or empty when there is nothing to summarize (no active plot)
     */
    public static Optional<DailySummaryDraft> summarize(HomeFacts facts) {
        return HomeQuestions.answer(QuestionId.HOME_WEEK_PLAN, facts).map(week -> new DailySummaryDraft(week.text(),
                List.of(windowCard(facts), week.facts().get(0), week.facts().get(1)), week.source()));
    }

    private static FactDraft windowCard(HomeFacts facts) {
        var today = facts.today();
        var open = HomeQuestions.openWindows(facts);
        if (!open.isEmpty()) {
            var closesOn = open.getFirst().windowClosesOn();
            // Today included, as the other window answers count it
            long daysLeft = ChronoUnit.DAYS.between(today, closesOn) + 1;
            return new FactDraft(shared("value.integer", daysLeft), shared("card.days_left.label", daysLeft),
                    shared("card.closes.pill", TextDate.shortOf(closesOn)), FactTone.PRIMARY);
        }
        var opening = HomeQuestions.windowsOpeningThisWeek(facts);
        if (!opening.isEmpty()) {
            var opensOn = opening.getFirst().windowOpensOn();
            long daysUntilOpen = ChronoUnit.DAYS.between(today, opensOn);
            return new FactDraft(shared("value.integer", daysUntilOpen), shared("card.days_to_open.label",
                    daysUntilOpen), shared("card.opens.pill", TextDate.shortOf(opensOn)), FactTone.PRIMARY);
        }
        return new FactDraft(shared("card.no_window.value"), shared("card.window.label"),
                shared("card.next_days.pill"), FactTone.PRIMARY);
    }
}
