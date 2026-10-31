package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué hago hoy en este lote?": three sentences, in this order: the thinning (the open window, then one still to
 * open, then a sampling left halfway), today's forecast against the heat and frost thresholds, and the open alerts.
 * Always answerable: without anything pending it says so.
 */
public final class PlotTodayFactsAssembler extends OnePlotFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.PLOT_TODAY;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData.PlotHome plot, HomeData home,
            SortedMap<String, Object> facts) {
        var today = data.today();
        var plan = plot.plan();
        var state = window(today, plan);
        putWindow(today, plan, state, facts);
        var sources = new ArrayList<CatalogText>();
        CatalogText thinning = switch (state) {
            case OPEN -> text(questionId(), "thinning.open", TextDate.of(plan.windowClosesOn()),
                    whole(plan.percentageToRemove()));
            case BEFORE -> text(questionId(), "thinning.before", TextDate.of(plan.windowOpensOn()));
            case SAMPLING -> {
                if (plot.treesNeeded() > 0) {
                    facts.put("treesNeeded", plot.treesNeeded());
                    yield text(questionId(), "thinning.sampling", plot.treesNeeded());
                }
                yield text(questionId(), "thinning.none");
            }
            case CLOSED, DONE, NONE -> text(questionId(), "thinning.none");
        };
        if (state != Window.NONE) {
            sources.add(shared("source.plan"));
        }

        var forecast = forecastToday(today, plot, home);
        final CatalogText weather;
        final FactCardTemplate weatherCard;
        if (forecast.isEmpty()) {
            weather = text(questionId(), "forecast.none");
            weatherCard = card(shared("card.no_forecast.value"), shared("card.today_max.label"),
                    shared("card.no_forecast.pill"), FactTone.PRIMARY);
        } else {
            var day = forecast.get();
            double max = oneDecimal(day.max());
            double min = oneDecimal(day.min());
            facts.put("todayMax", max);
            facts.put("todayMin", min);
            facts.put("heatThreshold", oneDecimal(home.thresholds().heatWarning()));
            facts.put("frostThreshold", oneDecimal(home.thresholds().frostWarning()));
            facts.put("heatReached", day.heatRisk());
            facts.put("frostReached", day.frostRisk());
            if (day.heatRisk()) {
                weather = text(questionId(), "forecast.heat", max, oneDecimal(home.thresholds().heatWarning()));
            } else if (day.frostRisk()) {
                weather = text(questionId(), "forecast.frost", min, oneDecimal(home.thresholds().frostWarning()));
            } else {
                weather = text(questionId(), "forecast.mild", min, max);
            }
            weatherCard = card(shared("value.measure", max, "°C"), shared("card.today_max.label"),
                    shared("card.today_min.pill", min), FactTone.PRIMARY);
            sources.add(forecastSource(plot.forecast()));
        }

        var alerts = home.openAlerts();
        facts.put("openAlerts", alerts.size());
        var severe = ClimateData.mostSevere(alerts);
        final CatalogText alertsText;
        if (severe.isEmpty()) {
            alertsText = text(questionId(), "alerts.none");
        } else {
            facts.put("incidentType", severe.get().type());
            facts.put("severity", severe.get().severity());
            alertsText = text(questionId(), "alerts.some", alerts.size(), AlertFactsAssembler.alertName(severe.get()),
                    AlertFactsAssembler.severityName(severe.get()));
        }
        sources.add(shared("source.open_alerts.plot"));
        var alertsCard = card(shared("value.integer", alerts.size()), shared("card.open_alerts.label", alerts.size()),
                shared("card.this_plot.pill"), FactTone.HIGHLIGHT);
        return Optional.of(new AssembledFacts(facts, text(questionId(), "text", thinning, weather, alertsText),
                List.of(weatherCard, alertsCard), sources, List.of()));
    }
}
