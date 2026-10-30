package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Predicate;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cuándo termina?": until when the forecast keeps the heat or frost of the alert, from the flags Telemetry set
 * on each forecast day. A hydric alert has no answer: the forecast does not anticipate soil moisture.
 */
public final class AlertWhenEndsFactsAssembler extends AlertFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALERT_WHEN_ENDS;
    }

    @Override
    protected Optional<AssembledFacts> build(ClimateData climate, ClimateData.Incident incident,
            SortedMap<String, Object> facts) {
        var forecast = climate.forecast();
        if (forecast == null || forecast.days().isEmpty() || climate.thresholds() == null) {
            return Optional.empty();
        }
        final Predicate<ForecastDay> atRisk;
        final double threshold;
        if (ClimateData.HEAT_WAVE.equals(incident.type())) {
            atRisk = ForecastDay::heatRisk;
            threshold = climate.thresholds().heatWarning();
        } else if (ClimateData.FROST_WARNING.equals(incident.type())) {
            atRisk = ForecastDay::frostRisk;
            threshold = climate.thresholds().frostWarning();
        } else {
            return Optional.empty();
        }
        var span = ForecastSpan.of(forecast.days(), atRisk);
        facts.put("riskThreshold", threshold);
        span.putFacts(facts, "riskDays", "lastRiskOn");

        var name = alertName(incident);
        final CatalogText text;
        final CatalogText endValue;
        switch (span.state()) {
            case ENDS -> {
                text = text(questionId(), "text.ends", name, TextDate.of(span.lastRiskOn()), span.riskDays(),
                        span.days(), threshold, TextDate.of(span.endsOn()));
                endValue = shared("card.ends.value", TextDate.shortOf(span.endsOn()));
            }
            case BEYOND -> {
                text = text(questionId(), "text.beyond", name, TextDate.of(span.lastRiskOn()), span.riskDays(),
                        span.days(), threshold);
                endValue = shared("card.ends.beyond");
            }
            default -> {
                text = text(questionId(), "text.clear", name, span.days(), threshold);
                endValue = shared("card.ends.clear");
            }
        }
        return Optional.of(new AssembledFacts(facts, text, List.of(
                card(shared("value.integer", span.riskDays()), shared("card.risk_days.label", span.riskDays()),
                        shared("card.threshold.pill", threshold, "°C"), FactTone.PRIMARY),
                card(endValue, shared("card.ends.label"), shared("card.forecast_days.pill", span.days()),
                        FactTone.HIGHLIGHT)),
                List.of(forecastSource(forecast)), List.of()));
    }
}
