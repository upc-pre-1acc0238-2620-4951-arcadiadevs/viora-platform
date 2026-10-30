package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * Base of the ALERT questions. Every one is about the same open alert of the plot, so they share its facts (type,
 * severity, the value against the threshold Telemetry used and the day it was raised) and its cards.
 */
abstract class AlertFactsAssembler implements FactsAssembler {

    @Override
    public final Optional<AssembledFacts> assemble(PlotAnswerData data) {
        var incident = data.climate().incident();
        if (incident == null) {
            return Optional.empty();
        }
        var facts = new TreeMap<String, Object>();
        facts.put("incidentType", incident.type());
        facts.put("severity", incident.severity());
        facts.put("alertValue", oneDecimal(incident.value()));
        facts.put("alertThreshold", oneDecimal(incident.threshold()));
        facts.put("unit", incident.unit());
        facts.put("alertRaisedOn", incident.triggeredOn().toString());
        return build(data.climate(), incident, facts);
    }

    /**
     * Builds the answer of the question.
     *
     * @param climate  the climate data of the plot
     * @param incident the open alert
     * @param facts    the shared facts; the question adds what it reads
     * @return the facts of the answer, or empty when a figure is missing
     */
    protected abstract Optional<AssembledFacts> build(ClimateData climate, ClimateData.Incident incident,
            SortedMap<String, Object> facts);

    /** "golpe de calor", "helada", "estrés hídrico". */
    static CatalogText alertName(ClimateData.Incident incident) {
        return shared("alert." + incident.type());
    }

    static CatalogText severityName(ClimateData.Incident incident) {
        return shared("severity." + incident.severity());
    }

    /** The value that breached the threshold, with the threshold as pill. */
    static FactCardTemplate valueCard(ClimateData.Incident incident) {
        return card(shared("value.measure", oneDecimal(incident.value()), incident.unit()),
                shared("card.alert_value.label." + incident.type()),
                shared("card.threshold.pill", oneDecimal(incident.threshold()), incident.unit()), FactTone.PRIMARY);
    }

    /** The level of the alert, with the day it was raised as pill. */
    static FactCardTemplate severityCard(ClimateData.Incident incident) {
        return card(shared("card.severity.value." + incident.severity()), shared("card.severity.label"),
                shared("card.since.pill", TextDate.shortOf(incident.triggeredOn())), FactTone.HIGHLIGHT);
    }

    static CatalogText alertSource(ClimateData.Incident incident) {
        return shared("source.alert", TextDate.of(incident.triggeredOn()));
    }
}
