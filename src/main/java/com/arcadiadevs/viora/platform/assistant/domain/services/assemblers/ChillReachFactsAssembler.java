package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.card;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.text;

/**
 * "¿Cuándo llego a las 30 porciones?": the day the requirement was reached, or the day Phenology projects from the
 * recent pace, against the budbreak of August 31 (fecha estimada 4 ago vs brotación 31 ago). Without enough days
 * to project there is no date to give.
 */
public final class ChillReachFactsAssembler extends ChillFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CHILL_REACH_30;
    }

    @Override
    protected Optional<AssembledFacts> build(ChillData chill, ChillData.Season season,
            SortedMap<String, Object> facts) {
        double threshold = threshold(season);
        double accumulated = accumulated(season);
        var budbreak = season.seasonEnd();
        facts.put("budbreakOn", budbreak.toString());
        facts.put("projectionStatus", season.projectionStatus());
        final CatalogText text;
        final CatalogText dateValue;
        final CatalogText dateLabel;
        final CatalogText datePill;
        final CatalogText budbreakPill;
        if (season.completed()) {
            long margin = ChronoUnit.DAYS.between(season.completionDate(), budbreak);
            facts.put("completionDate", season.completionDate().toString());
            facts.put("marginDays", margin);
            text = text(questionId(), "text.reached", threshold, TextDate.of(season.completionDate()), margin,
                    TextDate.of(budbreak));
            dateValue = date(season.completionDate());
            dateLabel = shared("card.reached_on.label");
            datePill = shared("card.chill_requirement.pill", threshold);
            budbreakPill = shared("card.margin.pill", margin);
        } else if (season.closed()) {
            double missing = missing(season);
            facts.put("missingPortions", missing);
            text = text(questionId(), "text.closed.short", String.valueOf(season.seasonYear()),
                    TextDate.of(budbreak), accumulated, threshold);
            dateValue = shared("card.no_date.value");
            dateLabel = shared("card.reached_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.short_by.pill", missing);
        } else if (ChillData.Season.PROJECTED.equals(season.projectionStatus())
                && season.projectedCompletionDate() != null) {
            long margin = ChronoUnit.DAYS.between(season.projectedCompletionDate(), budbreak);
            facts.put("projectedCompletionDate", season.projectedCompletionDate().toString());
            facts.put("marginDays", margin);
            text = text(questionId(), "text.projected", threshold, TextDate.of(season.projectedCompletionDate()),
                    margin, TextDate.of(budbreak));
            dateValue = date(season.projectedCompletionDate());
            dateLabel = shared("card.estimated_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.margin.pill", margin);
        } else if (ChillData.Season.NOT_REACHABLE_IN_SEASON.equals(season.projectionStatus())) {
            double missing = missing(season);
            facts.put("missingPortions", missing);
            text = text(questionId(), "text.not_reachable", threshold, TextDate.of(budbreak), accumulated, missing);
            dateValue = shared("card.no_date.value");
            dateLabel = shared("card.estimated_on.label");
            datePill = shared("card.portions_so_far.pill", accumulated);
            budbreakPill = shared("card.short_by.pill", missing);
        } else {
            // Too few days evaluated to project: the catalog disables the question
            return Optional.empty();
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(dateValue, dateLabel, datePill, FactTone.PRIMARY),
                card(date(budbreak), shared("card.budbreak.label"), budbreakPill, FactTone.HIGHLIGHT)),
                List.of(source(season)), List.of()));
    }
}
