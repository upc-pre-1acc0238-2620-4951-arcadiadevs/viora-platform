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
 * "¿Qué pasa si no llego a las 30 porciones?": with too little chill, budbreak and bloom may come later and uneven,
 * worded conservatively (pending validation by an agronomist) and always pointing to the technician. Viora does not
 * estimate the effect; a season that reached the requirement says so.
 */
public final class ChillIfIncompleteFactsAssembler extends ChillFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CHILL_IF_INCOMPLETE;
    }

    @Override
    protected Optional<AssembledFacts> build(ChillData chill, ChillData.Season season,
            SortedMap<String, Object> facts) {
        double accumulated = accumulated(season);
        double threshold = threshold(season);
        double missing = missing(season);
        var budbreak = TextDate.of(season.seasonEnd());
        facts.put("missingPortions", missing);
        facts.put("seasonState", season.state());
        facts.put("projectionStatus", season.projectionStatus());
        facts.put("budbreakOn", season.seasonEnd().toString());
        final CatalogText text;
        if (season.completed()) {
            facts.put("completionDate", season.completionDate().toString());
            var completedOn = TextDate.of(season.completionDate());
            text = season.closed()
                    ? text(questionId(), "text.closed.completed", String.valueOf(season.seasonYear()), threshold,
                            completedOn)
                    : text(questionId(), "text.completed", threshold, completedOn);
        } else if (season.closed()) {
            text = text(questionId(), "text.closed.short", String.valueOf(season.seasonYear()), accumulated,
                    threshold, missing);
        } else if (ChillData.Season.NOT_REACHABLE_IN_SEASON.equals(season.projectionStatus())) {
            text = text(questionId(), "text.at_risk", threshold, budbreak, accumulated);
        } else {
            text = text(questionId(), "text.pending", threshold, budbreak, accumulated);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(portionsCard(season), requirementCard(season)),
                List.of(source(season)), List.of()));
    }
}
