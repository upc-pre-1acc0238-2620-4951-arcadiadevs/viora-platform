package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Predicate;

/**
 * How long a risk lasts in the forecast, from the flags Telemetry set on each day: the days at risk, the last one and
 * the day after it. The risk is said to end only after its last day in the forecast, so a risk that comes back is
 * never reported as over.
 *
 * @param state      where the risk stands in the forecast
 * @param days       days the forecast covers
 * @param riskDays   days at risk
 * @param lastRiskOn last day at risk; null when none
 * @param endsOn     first day after the last one at risk; null when none or beyond the forecast
 */
record ForecastSpan(State state, int days, int riskDays, LocalDate lastRiskOn, LocalDate endsOn) {

    /** Where the risk stands in the forecast. */
    enum State {
        /** No day at risk. */
        CLEAR,
        /** The last day at risk is followed by days without it. */
        ENDS,
        /** The last forecast day is still at risk. */
        BEYOND
    }

    static ForecastSpan of(List<ForecastDay> forecast, Predicate<ForecastDay> atRisk) {
        int riskDays = (int) forecast.stream().filter(atRisk).count();
        int last = -1;
        for (int i = 0; i < forecast.size(); i++) {
            if (atRisk.test(forecast.get(i))) {
                last = i;
            }
        }
        if (last < 0) {
            return new ForecastSpan(State.CLEAR, forecast.size(), 0, null, null);
        }
        var lastRiskOn = forecast.get(last).date();
        if (last == forecast.size() - 1) {
            return new ForecastSpan(State.BEYOND, forecast.size(), riskDays, lastRiskOn, null);
        }
        return new ForecastSpan(State.ENDS, forecast.size(), riskDays, lastRiskOn, forecast.get(last + 1).date());
    }

    /** Puts the span in the facts under the given names. */
    void putFacts(SortedMap<String, Object> facts, String riskDaysName, String lastRiskName) {
        facts.put("forecastDays", days);
        facts.put(riskDaysName, riskDays);
        if (lastRiskOn != null) {
            facts.put(lastRiskName, lastRiskOn.toString());
        }
        if (endsOn != null) {
            facts.put("endsOn", endsOn.toString());
        }
    }
}
