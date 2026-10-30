package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué alertas afectan a este lote?": the open heat, frost and hydric alerts of the plot (or of every active plot
 * of the producer), and the most severe one against the threshold Telemetry used. Without alerts it says so.
 */
public final class HomeAlertsAffectingFactsAssembler extends HomeFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.HOME_ALERTS_AFFECTING;
    }

    @Override
    protected Optional<AssembledFacts> build(PlotAnswerData data, HomeData home, SortedMap<String, Object> facts) {
        var alerts = home.openAlerts();
        int plotsWithAlerts = home.plotsWithAlerts();
        int active = home.activePlots();
        boolean single = home.singlePlot();
        facts.put("openAlerts", alerts.size());
        facts.put("plotsWithAlerts", plotsWithAlerts);
        var severe = ClimateData.mostSevere(alerts);
        if (severe.isEmpty()) {
            var stateCard = single
                    ? card(shared("card.no_alerts.value"), shared("card.plot_state.label"),
                            shared("card.this_plot.pill"), FactTone.HIGHLIGHT)
                    : card(shared("value.integer", active), shared("card.field_plots.label", active),
                            shared("card.own_plots.pill"), FactTone.HIGHLIGHT);
            return Optional.of(new AssembledFacts(facts,
                    text(questionId(), single ? "text.none" : "text.none.field"),
                    List.of(card(shared("value.integer", 0), shared("card.open_alerts.label", 0),
                            shared("card.alert_types.pill"), FactTone.PRIMARY), stateCard),
                    List.of(shared("source.open_alerts")), List.of()));
        }
        var incident = severe.get();
        double value = oneDecimal(incident.value());
        double threshold = oneDecimal(incident.threshold());
        facts.put("incidentType", incident.type());
        facts.put("severity", incident.severity());
        facts.put("alertValue", value);
        facts.put("alertThreshold", threshold);
        facts.put("unit", incident.unit());
        facts.put("alertRaisedOn", incident.triggeredOn().toString());
        var name = AlertFactsAssembler.alertName(incident);
        var severity = AlertFactsAssembler.severityName(incident);
        var text = single
                ? text(questionId(), "text.some", alerts.size(), name, severity, value, incident.unit(), threshold)
                : text(questionId(), "text.some.field", alerts.size(), name, severity, value, incident.unit(),
                        threshold, plotsWithAlerts, active);
        var countCard = card(shared("value.integer", alerts.size()), shared("card.open_alerts.label", alerts.size()),
                single
                        ? shared("card.since.pill", TextDate.shortOf(incident.triggeredOn()))
                        : shared("card.in_plots.pill", plotsWithAlerts),
                FactTone.HIGHLIGHT);
        return Optional.of(new AssembledFacts(facts, text,
                List.of(AlertFactsAssembler.valueCard(incident), countCard),
                List.of(single ? AlertFactsAssembler.alertSource(incident) : shared("source.open_alerts")),
                List.of()));
    }
}
