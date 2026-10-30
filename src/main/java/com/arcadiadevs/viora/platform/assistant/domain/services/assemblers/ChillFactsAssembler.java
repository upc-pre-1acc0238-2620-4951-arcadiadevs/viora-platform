package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.card;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.oneDecimal;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.shared;

/**
 * Base of the WINTER_CHILL questions, about the season the chill metric describes (the winter in progress, otherwise
 * the last finished one). Portions are shown with one decimal and what is missing is the requirement minus the shown
 * portions, so both figures add up to the requirement. August 31, the end of the season, is the budbreak date of the
 * screen (contract).
 */
abstract class ChillFactsAssembler implements FactsAssembler {

    @Override
    public final Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var chill = data.chill();
        if (!chill.evaluated()) {
            // Not a chill question, or no day of the season evaluated yet: nothing to answer from
            return Optional.empty();
        }
        var season = chill.current();
        var facts = new TreeMap<String, Object>();
        facts.put("seasonYear", season.seasonYear());
        facts.put("accumulatedPortions", accumulated(season));
        facts.put("thresholdPortions", threshold(season));
        facts.put("evaluatedThrough", season.evaluatedThrough().toString());
        return build(chill, season, facts);
    }

    /**
     * Builds the answer of the question.
     *
     * @param chill  the current and last winter of the plot
     * @param season the season the screen is about, with at least one evaluated day
     * @param facts  the shared facts; the question adds what it reads
     * @return the facts of the answer, or empty when a figure is missing
     */
    protected abstract Optional<AssembledFacts> build(ChillData chill, ChillData.Season season,
            SortedMap<String, Object> facts);

    static double accumulated(ChillData.Season season) {
        return oneDecimal(season.accumulatedPortions());
    }

    static double threshold(ChillData.Season season) {
        return oneDecimal(season.thresholdPortions());
    }

    /** The shown requirement minus the shown portions, never below zero. */
    static double missing(ChillData.Season season) {
        var missing = BigDecimal.valueOf(threshold(season)).subtract(BigDecimal.valueOf(accumulated(season)));
        return missing.max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el 20 de julio". */
    static CatalogText source(ChillData.Season season) {
        return shared("source.chill", TextDate.of(season.evaluatedThrough()));
    }

    /** "24,4/30 · porciones de frío · hasta el 20 jul". */
    static FactCardTemplate portionsCard(ChillData.Season season) {
        return card(shared("value.portions", accumulated(season), threshold(season)),
                shared("card.chill_portions.label"), shared("card.until.pill", TextDate.shortOf(
                        season.evaluatedThrough())), FactTone.PRIMARY);
    }

    /** "Completo · requisito de frío · el 4 ago", or "5,6 · porciones por completar · requisito 30". */
    static FactCardTemplate requirementCard(ChillData.Season season) {
        if (season.completed()) {
            return card(shared("card.chill_completed.value"), shared("card.chill_requirement.label"),
                    shared("card.on.pill", TextDate.shortOf(season.completionDate())), FactTone.HIGHLIGHT);
        }
        return card(shared("value.one_decimal", missing(season)), shared("card.chill_missing.label"),
                shared("card.chill_requirement.pill", threshold(season)), FactTone.HIGHLIGHT);
    }

    static CatalogText date(LocalDate date) {
        return shared("card.date.value", TextDate.shortOf(date));
    }
}
