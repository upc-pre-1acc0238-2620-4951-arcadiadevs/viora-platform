package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo fue la campaña frente a las anteriores?": the harvest of the closed campaign (the latest one recorded)
 * against the mean of the earlier ones, as {@code alternation.vs_campaigns} compares them. With the surface of the
 * plot both are also given in tonnes per hectare, the unit of the contract, over the same polygon area the agronomic
 * dossier uses; the percentage is always computed from the kilograms, so the rounding of the tonnes never moves it.
 */
public final class CampaignVsPreviousFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CAMPAIGN_VS_PREVIOUS;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var yields = plot.yields();
        if (yields.size() < 2) {
            return Optional.empty();
        }
        int lastYear = yields.lastKey();
        double last = yields.get(lastYear);
        var earlier = yields.headMap(lastYear);
        double average = earlier.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (average <= 0.0) {
            return Optional.empty();
        }
        long difference = whole(Math.abs(last - average) / average * 100.0);
        String trend = difference == 0L ? "EQUAL" : last > average ? "ABOVE" : "BELOW";
        long lastKg = whole(last);
        long averageKg = whole(average);
        int previous = earlier.size();
        String year = String.valueOf(lastYear);
        facts.put("harvestCampaigns", yields.size());
        facts.put("lastCampaignYear", lastYear);
        facts.put("lastCampaignKg", lastKg);
        facts.put("previousCampaigns", previous);
        facts.put("previousAverageKg", averageKg);
        facts.put("differencePercent", difference);
        facts.put("trend", trend);
        String variant = trend.toLowerCase(Locale.ROOT);
        var sources = new ArrayList<CatalogText>(List.of(shared("source.campaigns", yields.size())));
        final CatalogText text;
        final FactCardTemplate harvestCard;
        Double area = home.areaHectares();
        if (area != null && area > 0.0) {
            double hectares = twoDecimals(area);
            double lastTons = oneDecimal(last / 1000.0 / area);
            double averageTons = oneDecimal(average / 1000.0 / area);
            facts.put("areaHectares", hectares);
            facts.put("lastCampaignTonsPerHa", lastTons);
            facts.put("previousAverageTonsPerHa", averageTons);
            text = text(questionId(), "text." + variant, year, lastTons, lastKg, difference, previous, averageTons);
            harvestCard = card(shared("value.t_per_ha", lastTons), shared("card.harvest.label", year),
                    shared("card.average_t_per_ha.pill", averageTons), FactTone.PRIMARY);
            sources.add(shared("source.plot_area", hectares));
        } else {
            text = text(questionId(), "text.kg." + variant, year, lastKg, difference, previous, averageKg);
            harvestCard = card(shared("value.kg", lastKg), shared("card.harvest.label", year),
                    shared("card.average.pill", averageKg), FactTone.PRIMARY);
        }
        String valueKey = switch (trend) {
            case "ABOVE" -> "value.plus_percent";
            case "BELOW" -> "value.minus_percent";
            default -> "value.percent";
        };
        return Optional.of(new AssembledFacts(facts, text, List.of(harvestCard,
                card(shared(valueKey, difference), shared("card.vs_average.label"),
                        shared("card.previous.pill", previous), FactTone.HIGHLIGHT)),
                sources, List.of()));
    }
}
