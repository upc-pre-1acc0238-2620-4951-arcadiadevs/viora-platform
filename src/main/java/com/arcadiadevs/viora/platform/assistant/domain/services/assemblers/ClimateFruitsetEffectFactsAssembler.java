package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo afecta el clima al cuajado?": the platform has no model of the effect of heat on fruit set, so the answer
 * gives only the hottest forecast day and the days at the heat threshold, and says the effect is not estimated
 * (pending agronomist validation).
 */
public final class ClimateFruitsetEffectFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CLIMATE_FRUITSET_EFFECT;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        if (climate.forecast() == null || climate.forecast().days().isEmpty() || climate.thresholds() == null) {
            return Optional.empty();
        }
        var days = climate.forecast().days();
        // The first of the hottest days
        var hottest = days.stream().max(Comparator.comparingDouble(ForecastDay::max)
                .thenComparing(ForecastDay::date, Comparator.reverseOrder())).orElseThrow();
        int heatDays = (int) days.stream().filter(ForecastDay::heatRisk).count();
        double max = oneDecimal(hottest.max());
        double threshold = climate.thresholds().heatWarning();

        var facts = new TreeMap<String, Object>();
        facts.put("hottestMax", max);
        facts.put("hottestOn", hottest.date().toString());
        facts.put("heatThreshold", threshold);
        facts.put("heatDays", heatDays);
        facts.put("forecastDays", days.size());
        var text = text(questionId(), heatDays > 0 ? "text.above" : "text.below", max, TextDate.of(hottest.date()),
                heatDays, days.size(), threshold);
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.measure", max, "°C"), shared("card.hottest.label"),
                        shared("card.on.pill", TextDate.shortOf(hottest.date())), FactTone.PRIMARY),
                card(shared("value.integer", heatDays), shared("card.heat_days.label", heatDays),
                        shared("card.threshold.pill", threshold, "°C"), FactTone.HIGHLIGHT)),
                List.of(forecastSource(climate.forecast())), List.of()));
    }
}
