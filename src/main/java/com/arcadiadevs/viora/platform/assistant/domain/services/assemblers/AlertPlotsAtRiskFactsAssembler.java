package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué otros lotes están en riesgo?": how many active plots of the producer have an open alert of the same type,
 * this one included. Only the producer's own plots are counted.
 */
public final class AlertPlotsAtRiskFactsAssembler extends AlertFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALERT_PLOTS_AT_RISK;
    }

    @Override
    protected Optional<AssembledFacts> build(ClimateData climate, ClimateData.Incident incident,
            SortedMap<String, Object> facts) {
        int atRisk = climate.plotsAtRisk();
        int active = climate.activePlots();
        if (atRisk < 1 || active < atRisk) {
            return Optional.empty();
        }
        facts.put("plotsAtRisk", atRisk);
        facts.put("activePlots", active);
        var name = alertName(incident);
        final CatalogText text;
        if (active == 1) {
            text = text(questionId(), "text.single", name);
        } else if (atRisk == 1) {
            text = text(questionId(), "text.only", name, active);
        } else {
            text = text(questionId(), "text.many", name, active, atRisk);
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.integer", atRisk), shared("card.plots_at_risk.label", atRisk), name,
                        FactTone.PRIMARY),
                card(shared("value.integer", active), shared("card.active_plots.label", active),
                        shared("card.own_plots.pill"), FactTone.HIGHLIGHT)),
                List.of(shared("source.open_alerts")), List.of()));
    }
}
