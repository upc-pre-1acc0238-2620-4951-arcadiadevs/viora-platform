package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.card;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.oneDecimal;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.text;

/**
 * "¿Cómo va frente al invierno pasado?": while the season is in progress, the portions so far against those of last
 * winter on the same date, and how last winter ended; once finished, both whole seasons and their warm days.
 */
public final class ChillVsLastWinterFactsAssembler extends ChillFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CHILL_VS_LAST_WINTER;
    }

    @Override
    protected Optional<AssembledFacts> build(ChillData chill, ChillData.Season season,
            SortedMap<String, Object> facts) {
        if (!chill.lastWinterEvaluated()) {
            return Optional.empty();
        }
        var last = chill.lastWinter();
        double accumulated = accumulated(season);
        double lastTotal = oneDecimal(last.accumulatedPortions());
        double threshold = threshold(season);
        String year = String.valueOf(season.seasonYear());
        String lastYear = String.valueOf(last.seasonYear());
        facts.put("lastWinterYear", last.seasonYear());
        facts.put("lastWinterPortions", lastTotal);
        if (last.completed()) {
            facts.put("lastWinterCompletionDate", last.completionDate().toString());
        }
        if (season.closed()) {
            facts.put("daysAbove24Celsius", season.daysAbove24Celsius());
            facts.put("lastWinterDaysAbove24Celsius", last.daysAbove24Celsius());
            facts.put("warmDayCelsius", season.warmDayCelsius());
            var text = text(questionId(), "text.closed", year, accumulated, lastYear, lastTotal,
                    season.daysAbove24Celsius(), season.warmDayCelsius(), last.daysAbove24Celsius());
            return Optional.of(new AssembledFacts(facts, text, List.of(
                    card(shared("value.one_decimal", accumulated), shared("card.season_portions.label", year),
                            shared("card.warm_days.pill", season.daysAbove24Celsius(), season.warmDayCelsius()),
                            FactTone.PRIMARY),
                    card(shared("value.one_decimal", lastTotal), shared("card.season_portions.label", lastYear),
                            shared("card.warm_days.pill", last.daysAbove24Celsius(), last.warmDayCelsius()),
                            FactTone.HIGHLIGHT)),
                    List.of(source(season)), List.of()));
        }
        var closing = last.completed()
                ? text(questionId(), "closing.completed", threshold, TextDate.of(last.completionDate()))
                : text(questionId(), "closing.short", lastTotal, threshold);
        var through = TextDate.of(season.evaluatedThrough());
        Double sameDate = last.portionsOn(season.evaluatedThrough().minusYears(1));
        final CatalogText text;
        final CatalogText lastValue;
        final CatalogText lastPill;
        if (sameDate != null) {
            double lastSameDate = oneDecimal(sameDate);
            facts.put("lastWinterSameDatePortions", lastSameDate);
            text = text(questionId(), "text.in_season", through, accumulated, lastSameDate, closing);
            lastValue = shared("value.one_decimal", lastSameDate);
            lastPill = shared("card.same_date.pill");
        } else {
            text = text(questionId(), "text.in_season.total", accumulated, through, closing);
            lastValue = shared("value.one_decimal", lastTotal);
            lastPill = shared("card.whole_season.pill");
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.one_decimal", accumulated), shared("card.this_winter.label"),
                        shared("card.until.pill", TextDate.shortOf(season.evaluatedThrough())), FactTone.PRIMARY),
                card(lastValue, shared("card.last_winter.label"), lastPill, FactTone.HIGHLIGHT)),
                List.of(source(season)), List.of()));
    }
}
