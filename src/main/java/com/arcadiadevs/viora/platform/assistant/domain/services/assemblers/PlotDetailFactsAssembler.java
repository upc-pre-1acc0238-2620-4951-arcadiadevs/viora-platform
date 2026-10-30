package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.whole;

/**
 * Base of the PLOT_DETAIL questions, about one plot read as HOME reads it. The thinning of the campaign is judged
 * with the window rules of the PLAN screen: the days left count today, the days until it opens do not.
 */
abstract class PlotDetailFactsAssembler implements FactsAssembler {

    /** Where the thinning of the campaign stands today. */
    enum Window {
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
        DONE
    }

    @Override
    public final Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var home = data.home();
        if (home.fieldScope() || !home.singlePlot()) {
            // Not a PLOT_DETAIL question: there is no plot to answer from
            return Optional.empty();
        }
        return build(data, home.plots().getFirst(), home, new TreeMap<>());
    }

    /**
     * Builds the answer of the question.
     *
     * @param data  the data of the question
     * @param plot  the plot
     * @param home  the plot with the alert thresholds
     * @param facts empty facts; the question adds what it reads
     * @return the facts of the answer, or empty when a figure is missing
     */
    protected abstract Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts);

    /**
     * Where the thinning of the campaign stands today.
     *
     * @param today the current day
     * @param plan  the plan of the campaign; may be null
     * @return the state
     */
    static Window window(LocalDate today, ThinningPlanData plan) {
        if (plan == null) {
            return Window.NONE;
        }
        if (plan.executedOn() != null) {
            return Window.DONE;
        }
        if (!plan.issued() || plan.percentageToRemove() == null || plan.windowOpensOn() == null
                || plan.windowClosesOn() == null) {
            return plan.sampledTrees() > 0 ? Window.SAMPLING : Window.NONE;
        }
        if (today.isBefore(plan.windowOpensOn())) {
            return Window.BEFORE;
        }
        return today.isAfter(plan.windowClosesOn()) ? Window.CLOSED : Window.OPEN;
    }

    /**
     * Adds the facts of the thinning window: its state and the figures that state shows.
     *
     * @param today the current day
     * @param plan  the plan of the campaign; may be null
     * @param state its state
     * @param facts the facts of the answer
     */
    static void putWindow(LocalDate today, ThinningPlanData plan, Window state, SortedMap<String, Object> facts) {
        facts.put("windowState", state.name());
        switch (state) {
            case BEFORE -> {
                facts.put("percentToRemove", whole(plan.percentageToRemove()));
                facts.put("windowOpensOn", plan.windowOpensOn().toString());
                facts.put("daysUntilOpen", daysUntilOpen(today, plan));
            }
            case OPEN -> {
                facts.put("percentToRemove", whole(plan.percentageToRemove()));
                facts.put("windowClosesOn", plan.windowClosesOn().toString());
                facts.put("daysLeft", daysLeft(today, plan));
            }
            case CLOSED -> facts.put("windowClosesOn", plan.windowClosesOn().toString());
            case DONE -> facts.put("thinningRecordedOn", plan.executedOn().toString());
            case SAMPLING -> facts.put("sampledTrees", plan.sampledTrees());
            case NONE -> {
                // Nothing sampled: the state says it all
            }
        }
    }

    /** Days left in the open window, today included. */
    static long daysLeft(LocalDate today, ThinningPlanData plan) {
        return ChronoUnit.DAYS.between(today, plan.windowClosesOn()) + 1;
    }

    /** Days until the window opens. */
    static long daysUntilOpen(LocalDate today, ThinningPlanData plan) {
        return ChronoUnit.DAYS.between(today, plan.windowOpensOn());
    }

    /** The forecast day of today, or empty without a forecast, its thresholds or today in it. */
    static Optional<ClimateData.ForecastDay> forecastToday(LocalDate today, HomeData.PlotHome plot, HomeData home) {
        if (plot.forecast() == null || home.thresholds() == null) {
            return Optional.empty();
        }
        return plot.forecast().days().stream().filter(day -> today.equals(day.date())).findFirst();
    }
}
