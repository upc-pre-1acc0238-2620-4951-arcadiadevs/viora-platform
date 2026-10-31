package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;

/**
 * Where the thinning of the campaign of one plot stands today, for the plot detail and sampling summary screens. It
 * follows the window rules of the PLAN screen: the days left count today, the days until it opens do not.
 */
enum ThinningWindow {

    /** No sampling this campaign. */
    NONE,
    /** Sampled, without an issued prescription yet. */
    SAMPLING,
    /** Prescription issued, window not open yet. */
    BEFORE,
    /** Window open today. */
    OPEN,
    /** Window closed without a recorded thinning. */
    CLOSED,
    /** Thinning recorded. */
    DONE;

    /**
     * Where the thinning of the campaign stands today.
     *
     * @param today the current day
     * @param plan  the plan of the campaign; may be null
     * @return the state
     */
    static ThinningWindow of(LocalDate today, ThinningPlanData plan) {
        if (plan == null) {
            return NONE;
        }
        if (plan.executedOn() != null) {
            return DONE;
        }
        if (!plan.issued() || plan.percentageToRemove() == null || plan.windowOpensOn() == null
                || plan.windowClosesOn() == null) {
            return plan.sampledTrees() > 0 ? SAMPLING : NONE;
        }
        if (today.isBefore(plan.windowOpensOn())) {
            return BEFORE;
        }
        return today.isAfter(plan.windowClosesOn()) ? CLOSED : OPEN;
    }

    /** Days left in the open window, today included. */
    static long daysLeft(LocalDate today, ThinningPlanData plan) {
        return ChronoUnit.DAYS.between(today, plan.windowClosesOn()) + 1;
    }

    /** Days until the window opens. */
    static long daysUntilOpen(LocalDate today, ThinningPlanData plan) {
        return ChronoUnit.DAYS.between(today, plan.windowOpensOn());
    }

    /**
     * The card of the thinning window: days left or until it opens, closed, recorded, or no plan yet.
     *
     * @param today the current day
     * @param plan  the plan of the campaign; may be null only in {@link #NONE}
     * @return the card
     */
    FactDraft card(LocalDate today, ThinningPlanData plan) {
        return switch (this) {
            case OPEN -> {
                long days = daysLeft(today, plan);
                yield new FactDraft(shared("value.integer", days), shared("card.days_left.label", days),
                        shared("card.window.open"), FactTone.HIGHLIGHT);
            }
            case BEFORE -> {
                long days = daysUntilOpen(today, plan);
                yield new FactDraft(shared("value.integer", days), shared("card.days_to_open.label", days),
                        shared("card.opens.pill", TextDate.shortOf(plan.windowOpensOn())), FactTone.HIGHLIGHT);
            }
            case CLOSED -> new FactDraft(shared("card.closed.value"), shared("card.window.label"),
                    shared("card.closed_on.pill", TextDate.shortOf(plan.windowClosesOn())), FactTone.HIGHLIGHT);
            case DONE -> new FactDraft(shared("card.done.value"), shared("card.done.label"),
                    shared("card.done.pill", TextDate.shortOf(plan.executedOn())), FactTone.HIGHLIGHT);
            case SAMPLING -> new FactDraft(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.sampling.pill", plan.sampledTrees()), FactTone.HIGHLIGHT);
            case NONE -> new FactDraft(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.no_plan.pill"), FactTone.HIGHLIGHT);
        };
    }
}
