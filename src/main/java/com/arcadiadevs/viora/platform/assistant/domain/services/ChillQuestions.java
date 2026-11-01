package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts.Season;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;

/**
 * The questions of the winter chill screen (P80, P133-P135): which ones it offers and why one is disabled, and the
 * template each one is answered with. They are about the season the chill metric describes (the winter in progress,
 * otherwise the last finished one). Portions are shown with one decimal and what is missing is the requirement minus
 * the shown portions, so both figures add up to the requirement. August 31, the end of the season, is the budbreak
 * date of the screen (contract).
 */
public final class ChillQuestions {

    private ChillQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. A finished winter is named by its
     * year in the labels. Without an evaluated day every question is disabled; the date needs 14 evaluated days to
     * be projected (or the requirement already reached) and the comparison needs last winter.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(ChillFacts facts) {
        if (!facts.evaluated()) {
            var missing = CatalogText.of("assistant.reason.chill_missing");
            return List.of(
                    question(QuestionId.CHILL_MISSING, label(QuestionId.CHILL_MISSING, "label"), "temp", missing),
                    question(QuestionId.CHILL_REACH_30, label(QuestionId.CHILL_REACH_30, "label_without_threshold"),
                            "calendar", missing),
                    question(QuestionId.CHILL_VS_LAST_WINTER, label(QuestionId.CHILL_VS_LAST_WINTER, "label"),
                            "history", missing),
                    question(QuestionId.CHILL_IF_INCOMPLETE,
                            label(QuestionId.CHILL_IF_INCOMPLETE, "label_without_threshold"), "alert", missing));
        }
        var current = facts.current();
        double threshold = current.thresholdPortions();
        boolean closed = current.closed();
        String year = String.valueOf(current.seasonYear());
        String lastYear = String.valueOf(current.seasonYear() - 1);
        CatalogText paceMissing = current.inProgress() && Season.INSUFFICIENT_DATA.equals(current.projectionStatus())
                ? CatalogText.of("assistant.reason.chill_pace_missing") : null;
        return List.of(
                question(QuestionId.CHILL_MISSING, closed
                        ? label(QuestionId.CHILL_MISSING, "label.closed", year)
                        : label(QuestionId.CHILL_MISSING, "label"), "temp", null),
                question(QuestionId.CHILL_REACH_30, closed
                        ? label(QuestionId.CHILL_REACH_30, "label.closed", threshold, year)
                        : label(QuestionId.CHILL_REACH_30, "label", threshold), "calendar", paceMissing),
                question(QuestionId.CHILL_VS_LAST_WINTER, closed
                        ? label(QuestionId.CHILL_VS_LAST_WINTER, "label.closed", year, lastYear)
                        : label(QuestionId.CHILL_VS_LAST_WINTER, "label"), "history",
                        facts.lastWinterEvaluated() ? null : CatalogText.of("assistant.reason.last_winter_missing")),
                question(QuestionId.CHILL_IF_INCOMPLETE, closed
                        ? label(QuestionId.CHILL_IF_INCOMPLETE, "label.closed", threshold, year)
                        : label(QuestionId.CHILL_IF_INCOMPLETE, "label", threshold), "alert", null));
    }

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, ChillFacts facts) {
        if (!facts.evaluated()) {
            // No day of the season evaluated yet: nothing to answer from
            return Optional.empty();
        }
        var season = facts.current();
        if (questionId.equals(QuestionId.CHILL_MISSING)) {
            return Optional.of(missing(season));
        } else if (questionId.equals(QuestionId.CHILL_REACH_30)) {
            return reach(season);
        } else if (questionId.equals(QuestionId.CHILL_VS_LAST_WINTER)) {
            return versusLastWinter(facts, season);
        } else if (questionId.equals(QuestionId.CHILL_IF_INCOMPLETE)) {
            return Optional.of(ifIncomplete(season));
        }
        return Optional.empty();
    }

    /**
     * "¿Cuánto frío me falta?": the portions accumulated against the varietal requirement and what is missing
     * (24/30, faltan 6). A halted season says that warm days stopped the accumulation; a finished one says how it
     * ended.
     */
    private static AnswerDraft missing(Season season) {
        var q = "chill.missing";
        double accumulated = accumulated(season);
        double threshold = threshold(season);
        double missing = missingPortions(season);
        var through = TextDate.of(season.evaluatedThrough());
        final CatalogText text;
        if (season.completed()) {
            var completedOn = TextDate.of(season.completionDate());
            text = season.closed()
                    ? text(q, "text.closed.completed", String.valueOf(season.seasonYear()), accumulated, threshold,
                            completedOn)
                    : text(q, "text.completed", threshold, completedOn, accumulated, through);
        } else if (season.closed()) {
            text = text(q, "text.closed.short", String.valueOf(season.seasonYear()), accumulated, threshold, missing,
                    through);
        } else if (Season.HALTED.equals(season.state())) {
            text = text(q, "text.halted", accumulated, threshold, missing, through, season.warmDayCelsius());
        } else {
            text = text(q, "text.pending", accumulated, threshold, missing, through);
        }
        return new AnswerDraft(text, List.of(portionsCard(season), requirementCard(season)), source(season),
                List.of());
    }

    /**
     * "¿Cuándo llego a las 30 porciones?": the day the requirement was reached, or the day Phenology projects from
     * the recent pace, against the budbreak of August 31 (fecha estimada 4 ago vs brotación 31 ago). Without enough
     * days to project there is no date to give.
     */
    private static Optional<AnswerDraft> reach(Season season) {
        var q = "chill.reach_30";
        double threshold = threshold(season);
        double accumulated = accumulated(season);
        var budbreak = season.seasonEnd();
        final CatalogText text;
        final CatalogText dateValue;
        final CatalogText dateLabel;
        final CatalogText datePill;
        final CatalogText budbreakPill;
        if (season.completed()) {
            long margin = ChronoUnit.DAYS.between(season.completionDate(), budbreak);
            text = text(q, "text.reached", threshold, TextDate.of(season.completionDate()), margin,
                    TextDate.of(budbreak));
            dateValue = date(season.completionDate());
            dateLabel = shared("card.reached_on.label");
            datePill = shared("card.chill_requirement.pill", threshold);
            budbreakPill = shared("card.margin.pill", margin);
        } else if (season.closed()) {
            text = text(q, "text.closed.short", String.valueOf(season.seasonYear()), TextDate.of(budbreak),
                    accumulated, threshold);
            dateValue = shared("card.no_date.value");
            dateLabel = shared("card.reached_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.short_by.pill", missingPortions(season));
        } else if (Season.PROJECTED.equals(season.projectionStatus()) && season.projectedCompletionDate() != null) {
            long margin = ChronoUnit.DAYS.between(season.projectedCompletionDate(), budbreak);
            text = text(q, "text.projected", threshold, TextDate.of(season.projectedCompletionDate()), margin,
                    TextDate.of(budbreak));
            dateValue = date(season.projectedCompletionDate());
            dateLabel = shared("card.estimated_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.margin.pill", margin);
        } else if (Season.NOT_REACHABLE_IN_SEASON.equals(season.projectionStatus())) {
            double missing = missingPortions(season);
            text = text(q, "text.not_reachable", threshold, TextDate.of(budbreak), accumulated, missing);
            dateValue = shared("card.no_date.value");
            dateLabel = shared("card.estimated_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.short_by.pill", missing);
        } else {
            // Too few days evaluated to project: the catalog disables the question
            return Optional.empty();
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(dateValue, dateLabel, datePill, FactTone.PRIMARY),
                new FactDraft(date(budbreak), shared("card.budbreak.label"), budbreakPill, FactTone.HIGHLIGHT)),
                source(season), List.of()));
    }

    /**
     * "¿Cómo va frente al invierno pasado?": while the season is in progress, the portions so far against those of
     * last winter on the same date, and how last winter ended; once finished, both whole seasons and their warm
     * days.
     */
    private static Optional<AnswerDraft> versusLastWinter(ChillFacts facts, Season season) {
        if (!facts.lastWinterEvaluated()) {
            return Optional.empty();
        }
        var q = "chill.vs_last_winter";
        var last = facts.lastWinter();
        double accumulated = accumulated(season);
        double lastTotal = round(last.accumulatedPortions(), 1);
        double threshold = threshold(season);
        String year = String.valueOf(season.seasonYear());
        String lastYear = String.valueOf(last.seasonYear());
        if (season.closed()) {
            var text = text(q, "text.closed", year, accumulated, lastYear, lastTotal, season.daysAbove24Celsius(),
                    season.warmDayCelsius(), last.daysAbove24Celsius());
            return Optional.of(new AnswerDraft(text, List.of(
                    new FactDraft(shared("value.one_decimal", accumulated), shared("card.season_portions.label", year),
                            shared("card.warm_days.pill", season.daysAbove24Celsius(), season.warmDayCelsius()),
                            FactTone.PRIMARY),
                    new FactDraft(shared("value.one_decimal", lastTotal),
                            shared("card.season_portions.label", lastYear),
                            shared("card.warm_days.pill", last.daysAbove24Celsius(), last.warmDayCelsius()),
                            FactTone.HIGHLIGHT)),
                    source(season), List.of()));
        }
        var closing = last.completed()
                ? text(q, "closing.completed", threshold, TextDate.of(last.completionDate()))
                : text(q, "closing.short", lastTotal, threshold);
        var through = TextDate.of(season.evaluatedThrough());
        Double sameDate = facts.lastWinterSameDatePortions();
        final CatalogText text;
        final CatalogText lastValue;
        final CatalogText lastPill;
        if (sameDate != null) {
            double lastSameDate = round(sameDate, 1);
            text = text(q, "text.in_season", through, accumulated, lastSameDate, closing);
            lastValue = shared("value.one_decimal", lastSameDate);
            lastPill = shared("card.same_date.pill");
        } else {
            text = text(q, "text.in_season.total", accumulated, through, closing);
            lastValue = shared("value.one_decimal", lastTotal);
            lastPill = shared("card.whole_season.pill");
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.one_decimal", accumulated), shared("card.this_winter.label"),
                        shared("card.until.pill", TextDate.shortOf(season.evaluatedThrough())), FactTone.PRIMARY),
                new FactDraft(lastValue, shared("card.last_winter.label"), lastPill, FactTone.HIGHLIGHT)),
                source(season), List.of()));
    }

    /**
     * "¿Qué pasa si no llego a las 30 porciones?": with too little chill, budbreak and bloom may come later and
     * uneven, worded conservatively (pending validation by an agronomist) and always pointing to the technician.
     * Viora does not estimate the effect; a season that reached the requirement says so.
     */
    private static AnswerDraft ifIncomplete(Season season) {
        var q = "chill.if_incomplete";
        double accumulated = accumulated(season);
        double threshold = threshold(season);
        var budbreak = TextDate.of(season.seasonEnd());
        final CatalogText text;
        if (season.completed()) {
            var completedOn = TextDate.of(season.completionDate());
            text = season.closed()
                    ? text(q, "text.closed.completed", String.valueOf(season.seasonYear()), threshold, completedOn)
                    : text(q, "text.completed", threshold, completedOn);
        } else if (season.closed()) {
            text = text(q, "text.closed.short", String.valueOf(season.seasonYear()), accumulated, threshold,
                    missingPortions(season));
        } else if (Season.NOT_REACHABLE_IN_SEASON.equals(season.projectionStatus())) {
            text = text(q, "text.at_risk", threshold, budbreak, accumulated);
        } else {
            text = text(q, "text.pending", threshold, budbreak, accumulated);
        }
        return new AnswerDraft(text, List.of(portionsCard(season), requirementCard(season)), source(season),
                List.of());
    }

    private static double accumulated(Season season) {
        return round(season.accumulatedPortions(), 1);
    }

    private static double threshold(Season season) {
        return round(season.thresholdPortions(), 1);
    }

    /** The shown requirement minus the shown portions, never below zero. */
    private static double missingPortions(Season season) {
        var missing = BigDecimal.valueOf(threshold(season)).subtract(BigDecimal.valueOf(accumulated(season)));
        return missing.max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el 20 de julio". */
    private static CatalogText source(Season season) {
        return shared("source.chill", TextDate.of(season.evaluatedThrough()));
    }

    /** "24,4/30 · porciones de frío · hasta el 20 jul". */
    private static FactDraft portionsCard(Season season) {
        return new FactDraft(shared("value.portions", accumulated(season), threshold(season)),
                shared("card.chill_portions.label"),
                shared("card.until.pill", TextDate.shortOf(season.evaluatedThrough())), FactTone.PRIMARY);
    }

    /** "Completo · requisito de frío · el 4 ago", or "5,6 · porciones por completar · requisito 30". */
    private static FactDraft requirementCard(Season season) {
        if (season.completed()) {
            return new FactDraft(shared("card.chill_completed.value"), shared("card.chill_requirement.label"),
                    shared("card.on.pill", TextDate.shortOf(season.completionDate())), FactTone.HIGHLIGHT);
        }
        return new FactDraft(shared("value.one_decimal", missingPortions(season)), shared("card.chill_missing.label"),
                shared("card.chill_requirement.pill", threshold(season)), FactTone.HIGHLIGHT);
    }

    private static CatalogText date(LocalDate date) {
        return shared("card.date.value", TextDate.shortOf(date));
    }
}
