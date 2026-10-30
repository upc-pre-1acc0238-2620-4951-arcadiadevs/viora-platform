package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerTexts.*;

/**
 * "¿Cómo afecta al cuajado?": the platform has no model of the effect of an alert on fruit set, so the answer gives
 * only the breach and says the effect is not estimated (pending agronomist validation).
 */
public final class AlertFruitsetEffectFactsAssembler extends AlertFactsAssembler {

    @Override
    public QuestionId questionId() {
        return QuestionId.ALERT_FRUITSET_EFFECT;
    }

    @Override
    protected Optional<AssembledFacts> build(ClimateData climate, ClimateData.Incident incident,
            SortedMap<String, Object> facts) {
        var text = text(questionId(), "text", alertName(incident), oneDecimal(incident.value()), incident.unit(),
                oneDecimal(incident.threshold()));
        return Optional.of(new AssembledFacts(facts, text, List.of(valueCard(incident), severityCard(incident)),
                List.of(alertSource(incident)), List.of()));
    }
}
