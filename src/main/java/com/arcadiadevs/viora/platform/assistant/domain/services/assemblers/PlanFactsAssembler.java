package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * Base of the PLAN questions. Every one reads the same issued prescription, so they share its facts (load, target,
 * percentage, window and sampling) and only add what their scenario derives from them.
 */
abstract class PlanFactsAssembler implements FactsAssembler {

    /** Where the thinning window stands today. */
    enum WindowState { BEFORE, OPEN, CLOSED, DONE }

    /**
     * The issued plan with its figures rounded as shown.
     *
     * @param plotId        the plot
     * @param percent       prescribed percentage, whole
     * @param measuredLoad  load of the sampling, unrounded, for derived scenarios
     * @param load          load of the sampling, two decimals
     * @param target        target load, two decimals
     * @param opensOn       window opening
     * @param closesOn      window closing
     * @param state         where the window stands today
     * @param daysLeft      days left in the open window, today included; null otherwise
     * @param daysUntilOpen days until the window opens; null otherwise
     * @param sampledTrees  trees sampled
     * @param lastSampledOn latest sampling day; may be null
     * @param executedOn    day the thinning was recorded; null while not recorded
     */
    record Plan(PlotId plotId, int percent, double measuredLoad, double load, double target, LocalDate opensOn,
            LocalDate closesOn, WindowState state, Long daysLeft, Long daysUntilOpen, int sampledTrees,
            LocalDate lastSampledOn, LocalDate executedOn) {
    }

    @Override
    public final Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var plan = data.plan();
        if (plan == null || !plan.issued() || plan.percentageToRemove() == null
                || plan.preThinningFruitsPerShoot() == null || plan.targetFruitsPerShoot() == null
                || plan.windowOpensOn() == null || plan.windowClosesOn() == null) {
            return Optional.empty();
        }
        var today = data.today();
        WindowState state;
        if (plan.executedOn() != null) {
            state = WindowState.DONE;
        } else if (today.isBefore(plan.windowOpensOn())) {
            state = WindowState.BEFORE;
        } else if (today.isAfter(plan.windowClosesOn())) {
            state = WindowState.CLOSED;
        } else {
            state = WindowState.OPEN;
        }
        var current = new Plan(data.plotId(), (int) whole(plan.percentageToRemove()),
                plan.preThinningFruitsPerShoot(), twoDecimals(plan.preThinningFruitsPerShoot()),
                twoDecimals(plan.targetFruitsPerShoot()), plan.windowOpensOn(), plan.windowClosesOn(), state,
                state == WindowState.OPEN ? ChronoUnit.DAYS.between(today, plan.windowClosesOn()) + 1 : null,
                state == WindowState.BEFORE ? ChronoUnit.DAYS.between(today, plan.windowOpensOn()) : null,
                plan.sampledTrees(), plan.lastSampledOn(), plan.executedOn());

        var facts = new TreeMap<String, Object>();
        facts.put("percentToRemove", current.percent());
        facts.put("preThinningLoad", current.load());
        facts.put("targetLoad", current.target());
        facts.put("windowOpensOn", current.opensOn().toString());
        facts.put("windowClosesOn", current.closesOn().toString());
        facts.put("windowState", state.name());
        facts.put("sampledTrees", current.sampledTrees());
        putIfPresent(facts, "daysLeft", current.daysLeft());
        putIfPresent(facts, "daysUntilOpen", current.daysUntilOpen());
        putIfPresent(facts, "lastSampledOn", current.lastSampledOn() == null ? null : current.lastSampledOn().toString());
        putIfPresent(facts, "thinningRecordedOn", current.executedOn() == null ? null : current.executedOn().toString());
        return Optional.of(build(current, facts));
    }

    /**
     * Builds the answer of the question.
     *
     * @param plan  the issued plan
     * @param facts the shared facts; the question adds what it derives
     * @return the facts of the answer
     */
    protected abstract AssembledFacts build(Plan plan, SortedMap<String, Object> facts);

    /** "Basado en 5 árboles, ronda del 29 de octubre". */
    protected static List<CatalogText> samplingSources(Plan plan) {
        return List.of(plan.lastSampledOn() == null
                ? shared("source.sampling_undated", plan.sampledTrees())
                : shared("source.sampling", plan.sampledTrees(), TextDate.of(plan.lastSampledOn())));
    }

    /** "Registrar aclareo" while the window is open and the thinning was not recorded yet. */
    protected static List<ActionTemplate> registerAction(Plan plan) {
        if (plan.state() != WindowState.OPEN) {
            return List.of();
        }
        return List.of(new ActionTemplate(ActionTemplate.DEEPLINK, shared("action.register_thinning"),
                "thinning/execute?plotId=" + plan.plotId().plotId()));
    }

    /** "0,57 · frutos por brote · estimado · objetivo 0,40". */
    protected static FactCardTemplate loadCard(Plan plan) {
        return card(shared("value.decimal", plan.load()), shared("card.load.label"),
                shared("card.target.pill", plan.target()), FactTone.PRIMARY);
    }

    /** "−30 % · de los frutos · quedan 12 días · ventana abierta". */
    protected static FactCardTemplate removeCard(Plan plan) {
        var label = plan.state() == WindowState.OPEN
                ? shared("card.remove.label.open", plan.daysLeft())
                : shared("card.remove.label");
        return card(shared("value.minus_percent", plan.percent()), label, windowPill(plan), FactTone.HIGHLIGHT);
    }

    protected static CatalogText windowPill(Plan plan) {
        return shared("card.window." + plan.state().name().toLowerCase(Locale.ROOT));
    }

    private static void putIfPresent(SortedMap<String, Object> facts, String key, Object value) {
        if (value != null) {
            facts.put(key, value);
        }
    }
}
