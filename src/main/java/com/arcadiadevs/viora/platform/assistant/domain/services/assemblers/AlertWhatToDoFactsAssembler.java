package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Qué hago ante este golpe de calor?": the breach against the threshold and the mitigation steps of the alert.
 * It points to the alert's own steps and adds no advice of its own.
 */
public final class AlertWhatToDoFactsAssembler extends AlertFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALERT_WHAT_TO_DO;
    }

    @Override
    protected Optional<AssembledFacts> build(ClimateData climate, ClimateData.Incident incident,
            SortedMap<String, Object> facts) {
        facts.put("mitigationSteps", incident.steps());
        facts.put("completedSteps", incident.completedSteps());
        var value = oneDecimal(incident.value());
        var threshold = oneDecimal(incident.threshold());
        var text = incident.steps() > 0
                ? text(questionId(), "text", alertName(incident), value, incident.unit(), threshold,
                        severityName(incident), incident.completedSteps(), incident.steps())
                : text(questionId(), "text.no_steps", alertName(incident), value, incident.unit(), threshold,
                        severityName(incident));
        return Optional.of(new AssembledFacts(facts, text, List.of(valueCard(incident), severityCard(incident)),
                List.of(alertSource(incident)), List.of()));
    }
}
