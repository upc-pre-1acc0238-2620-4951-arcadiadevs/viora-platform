package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuándo baja el calor?": the days the forecast reaches the heat threshold, as Telemetry flagged them, and the
 * first day after the last of them.
 */
public final class HeatEndsFactsAssembler implements FactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.CLIMATE_HEAT_ENDS;
    }

    @Override
    public Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var climate = data.climate();
        if (climate.forecast() == null || climate.forecast().days().isEmpty() || climate.thresholds() == null) {
            return Optional.empty();
        }
        var span = ForecastSpan.of(climate.forecast().days(), ForecastDay::heatRisk);
        double threshold = climate.thresholds().heatWarning();
        var facts = new TreeMap<String, Object>();
        facts.put("heatThreshold", threshold);
        span.putFacts(facts, "heatDays", "lastHeatOn");

        final CatalogText text;
        final CatalogText endValue;
        switch (span.state()) {
            case ENDS -> {
                text = text(questionId(), "text.ends", TextDate.of(span.lastRiskOn()), span.riskDays(), span.days(),
                        threshold, TextDate.of(span.endsOn()));
                endValue = shared("card.ends.value", TextDate.shortOf(span.endsOn()));
            }
            case BEYOND -> {
                text = text(questionId(), "text.beyond", TextDate.of(span.lastRiskOn()), span.riskDays(),
                        span.days(), threshold);
                endValue = shared("card.ends.beyond");
            }
            default -> {
                text = text(questionId(), "text.clear", span.days(), threshold);
                endValue = shared("card.ends.clear");
            }
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.integer", span.riskDays()), shared("card.heat_days.label", span.riskDays()),
                        shared("card.threshold.pill", threshold, "°C"), FactTone.PRIMARY),
                card(endValue, shared("card.ends.label"), shared("card.forecast_days.pill", span.days()),
                        FactTone.HIGHLIGHT)),
                List.of(forecastSource(climate.forecast())), List.of()));
    }
}
