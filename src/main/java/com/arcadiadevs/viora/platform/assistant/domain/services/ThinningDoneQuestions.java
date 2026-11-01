package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningDoneFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.whole;

/**
 * The questions of the screen shown after the thinning of a plot is recorded (P63, P145-P147): which ones it offers
 * and why one is disabled, and the template each one is answered with. No answer says anything about the next
 * campaign's harvest: no context projects it (docs/asistente-decisiones.md, Pantalla Aclareo registrado).
 */
public final class ThinningDoneQuestions {

    /** Load states with their own sentence. */
    private static final Set<String> LOAD_STATES = Set.of("BALANCED", "MODERATE_OVERLOAD", "SEVERE_OVERLOAD");

    /** Profile statuses with their own sentence; any other reads as "the profile of the variety". */
    private static final Set<String> PROFILE_STATUSES = Set.of("AGRONOMIST_APPROVED", "PROVISIONAL",
            "SYNTHETIC_DEMO");

    private ThinningDoneQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. Every question needs the thinning
     * recorded this campaign. The harvest change needs the yield Thinning estimates after it (3 earlier harvests and
     * the plot area) and the target needs the load balance of the confirmation; the next sampling is always offered
     * once the thinning is recorded.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(ThinningDoneFacts facts) {
        var outcome = facts.outcome();
        if (outcome == null) {
            var missing = CatalogText.of("assistant.reason.thinning_missing");
            return List.of(
                    question(QuestionId.THINNING_HARVEST_CHANGE, label(QuestionId.THINNING_HARVEST_CHANGE, "label"),
                            "harvest", missing),
                    question(QuestionId.THINNING_WHY_TARGET,
                            label(QuestionId.THINNING_WHY_TARGET, "label_without_target"), "prio", missing),
                    question(QuestionId.THINNING_NEXT_SAMPLING, label(QuestionId.THINNING_NEXT_SAMPLING, "label"),
                            "calendar", missing));
        }
        boolean hasTarget = outcome.targetLoad() != null;
        return List.of(
                question(QuestionId.THINNING_HARVEST_CHANGE, label(QuestionId.THINNING_HARVEST_CHANGE, "label"),
                        "harvest", outcome.kgPerHaAfterThinning() == null
                                ? CatalogText.of("assistant.reason.yield_missing") : null),
                question(QuestionId.THINNING_WHY_TARGET, hasTarget
                                ? label(QuestionId.THINNING_WHY_TARGET, "label", round(outcome.targetLoad(), 2))
                                : label(QuestionId.THINNING_WHY_TARGET, "label_without_target"), "prio",
                        hasTarget ? null : CatalogText.of("assistant.reason.facts_missing")),
                question(QuestionId.THINNING_NEXT_SAMPLING, label(QuestionId.THINNING_NEXT_SAMPLING, "label"),
                        "calendar", null));
    }

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, ThinningDoneFacts facts) {
        var outcome = facts.outcome();
        if (outcome == null) {
            return Optional.empty();
        }
        if (questionId.equals(QuestionId.THINNING_HARVEST_CHANGE)) {
            return harvestChange(outcome);
        } else if (questionId.equals(QuestionId.THINNING_WHY_TARGET)) {
            return whyTarget(outcome);
        } else if (questionId.equals(QuestionId.THINNING_NEXT_SAMPLING)) {
            return Optional.of(nextSampling(outcome, facts.campaignYear() + 1));
        }
        return Optional.empty();
    }

    /**
     * "¿Cómo cambia mi cosecha?": the yield Thinning estimates with the load the thinning left against the one with
     * the load before it, and where that load stands against the target. Without a caliber model the yield follows
     * the number of fruits, so the estimate without thinning is a ceiling and the text says so. The contract mockup
     * adds "2027 más estable"; no context projects the next campaign, so the answer never says it.
     */
    private static Optional<AnswerDraft> harvestChange(ThinningOutcomeData outcome) {
        var q = "thinning.harvest_change";
        if (outcome.kgPerHaAfterThinning() == null) {
            return Optional.empty();
        }
        long after = whole(outcome.kgPerHaAfterThinning());
        boolean ceiling = ThinningOutcomeData.FRUIT_COUNT_ONLY.equals(outcome.yieldBasis());
        final CatalogText yield;
        if (outcome.kgPerHaWithoutThinning() == null) {
            yield = text(q, "yield.only", after);
        } else {
            long without = whole(outcome.kgPerHaWithoutThinning());
            yield = ceiling ? text(q, "yield.ceiling", after, without) : text(q, "yield.compared", after, without);
        }
        final CatalogText text;
        final FactDraft loadCard;
        if (outcome.residualLoad() != null && outcome.targetLoad() != null
                && outcome.loadState() != null && LOAD_STATES.contains(outcome.loadState())) {
            double residual = round(outcome.residualLoad(), 2);
            double target = round(outcome.targetLoad(), 2);
            text = text(q, "text", yield, text(q, "load." + outcome.loadState(), residual, target));
            loadCard = new FactDraft(shared("value.decimal", residual), shared("card.residual_load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else {
            text = text(q, "text.single", yield);
            loadCard = new FactDraft(shared("card.date.value", TextDate.shortOf(outcome.executedOn())),
                    shared("card.done.label"), shared("card.window.done"), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.integer", after), shared("card.yield.label"),
                        ceiling ? shared("card.fruit_count.pill") : shared("card.calibrating.pill"),
                        FactTone.PRIMARY),
                loadCard), thinningSource(outcome), List.of()));
    }

    /**
     * "¿Por qué el objetivo es 0,40?": the target load is the one the thinning profile of the variety sets, the load
     * the tree can sustain; the answer says whether an agronomist approved that profile, and where the thinning left
     * the load. Thinning never derives the target from the plot, so the answer never says it does.
     */
    private static Optional<AnswerDraft> whyTarget(ThinningOutcomeData outcome) {
        var q = "thinning.why_target";
        if (outcome.targetLoad() == null) {
            return Optional.empty();
        }
        double target = round(outcome.targetLoad(), 2);
        var status = outcome.profileStatus() != null && PROFILE_STATUSES.contains(outcome.profileStatus())
                ? outcome.profileStatus() : null;
        var profile = status == null ? text(q, "profile.unknown") : text(q, "profile." + status);
        var targetCard = new FactDraft(shared("value.decimal", target), shared("card.target.label"),
                status == null ? shared("card.profile.pill") : shared("card.profile.pill." + status),
                FactTone.PRIMARY);
        final CatalogText text;
        final FactDraft resultCard;
        if (outcome.preThinningLoad() != null && outcome.residualLoad() != null
                && outcome.actualRemovalPercentage() != null) {
            double pre = round(outcome.preThinningLoad(), 2);
            double residual = round(outcome.residualLoad(), 2);
            long removed = whole(outcome.actualRemovalPercentage());
            text = text(q, "text", text(q, "target", target), profile, text(q, "result", pre, removed, residual));
            resultCard = new FactDraft(shared("value.decimal", residual), shared("card.residual_load.label"),
                    shared("card.removed.pill", removed), FactTone.HIGHLIGHT);
        } else {
            text = text(q, "text.short", text(q, "target", target), profile);
            resultCard = new FactDraft(shared("card.done.value"), shared("card.done.label"),
                    shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AnswerDraft(text, List.of(targetCard, resultCard), thinningSource(outcome),
                List.of()));
    }

    /**
     * "¿Cuándo vuelvo a muestrear?": with the thinning recorded this campaign needs no more sampling; the next one is
     * sampled after recording its full bloom, before the window the profile of the variety opens that many days
     * after it. Thinning has no other rule for the next sampling, so the answer gives no date.
     */
    private static AnswerDraft nextSampling(ThinningOutcomeData outcome, int nextCampaign) {
        var q = "thinning.next_sampling";
        var next = String.valueOf(nextCampaign);
        final CatalogText text;
        final FactDraft windowCard;
        if (outcome.windowOpensDaysAfterBloom() != null && outcome.windowClosesDaysAfterBloom() != null) {
            int opens = outcome.windowOpensDaysAfterBloom();
            int closes = outcome.windowClosesDaysAfterBloom();
            text = text(q, "text.window", TextDate.of(outcome.executedOn()), next, opens, closes);
            windowCard = new FactDraft(shared("value.integer_range", opens, closes),
                    shared("card.days_after_bloom.label"), shared("card.window.label"), FactTone.HIGHLIGHT);
        } else {
            text = text(q, "text", TextDate.of(outcome.executedOn()), next);
            windowCard = new FactDraft(shared("card.done.value"), shared("card.done.label"),
                    shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
        }
        return new AnswerDraft(text, List.of(
                new FactDraft(shared("card.date.value", next), shared("card.next_campaign.label"),
                        shared("card.after_bloom.pill"), FactTone.PRIMARY),
                windowCard), thinningSource(outcome), List.of());
    }

    /** "Basado en el aclareo registrado el 28 de octubre". */
    static CatalogText thinningSource(ThinningOutcomeData outcome) {
        return shared("source.thinning", TextDate.of(outcome.executedOn()));
    }
}
