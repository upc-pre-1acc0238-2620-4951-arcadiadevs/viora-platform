package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.text;

/**
 * "¿Cuánto frío me falta?": the portions accumulated against the varietal requirement and what is missing
 * (24/30, faltan 6). A halted season says that warm days stopped the accumulation; a finished one says how it ended.
 */
public final class ChillMissingFactsAssembler extends ChillFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CHILL_MISSING;
    }

    @Override
    protected Optional<AssembledFacts> build(ChillData chill, ChillData.Season season,
            SortedMap<String, Object> facts) {
        double accumulated = accumulated(season);
        double threshold = threshold(season);
        double missing = missing(season);
        var through = TextDate.of(season.evaluatedThrough());
        facts.put("missingPortions", missing);
        facts.put("seasonState", season.state());
        final CatalogText text;
        if (season.completed()) {
            facts.put("completionDate", season.completionDate().toString());
            var completedOn = TextDate.of(season.completionDate());
            text = season.closed()
                    ? text(questionId(), "text.closed.completed", String.valueOf(season.seasonYear()), accumulated,
                            threshold, completedOn)
                    : text(questionId(), "text.completed", threshold, completedOn, accumulated, through);
        } else if (season.closed()) {
            text = text(questionId(), "text.closed.short", String.valueOf(season.seasonYear()), accumulated,
                    threshold, missing, through);
        } else if (ChillData.Season.HALTED.equals(season.state())) {
            facts.put("warmDayCelsius", season.warmDayCelsius());
            text = text(questionId(), "text.halted", accumulated, threshold, missing, through,
                    season.warmDayCelsius());
        } else {
            text = text(questionId(), "text.pending", accumulated, threshold, missing, through);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(portionsCard(season), requirementCard(season)),
                List.of(source(season)), List.of()));
    }
}
