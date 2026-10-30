package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo va este lote frente a mis campañas?": the last registered harvest against the mean of the earlier ones,
 * in total kilograms as the producer registered them.
 */
public final class VsCampaignsFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALTERNATION_VS_CAMPAIGNS;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var yields = data.historicalYields();
        if (yields.size() < 2) {
            return Optional.empty();
        }
        int lastYear = yields.lastKey();
        double last = yields.get(lastYear);
        var earlier = yields.headMap(lastYear);
        double average = earlier.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        long difference = average > 0.0 ? whole(Math.abs(last - average) / average * 100.0) : 0L;
        String trend = difference == 0L ? "EQUAL" : last > average ? "ABOVE" : "BELOW";
        long lastKg = whole(last);
        long averageKg = whole(average);
        int previous = earlier.size();

        var facts = new TreeMap<String, Object>();
        facts.put("harvestCampaigns", yields.size());
        facts.put("lastCampaignYear", lastYear);
        facts.put("lastCampaignKg", lastKg);
        facts.put("previousCampaigns", previous);
        facts.put("previousAverageKg", averageKg);
        facts.put("differencePercent", difference);
        facts.put("trend", trend);
        String valueKey = switch (trend) {
            case "ABOVE" -> "value.plus_percent";
            case "BELOW" -> "value.minus_percent";
            default -> "value.percent";
        };
        return Optional.of(new AssembledFacts(facts,
                text(questionId(), "text." + trend.toLowerCase(Locale.ROOT),
                        String.valueOf(lastYear), lastKg, difference, previous, averageKg),
                List.of(
                        card(shared("value.kg", lastKg), shared("card.harvest.label", String.valueOf(lastYear)),
                                shared("card.average.pill", averageKg), FactTone.PRIMARY),
                        card(shared(valueKey, difference), shared("card.vs_average.label"),
                                shared("card.previous.pill", previous), FactTone.HIGHLIGHT)),
                List.of(shared("source.campaigns", yields.size())),
                List.of()));
    }
}
