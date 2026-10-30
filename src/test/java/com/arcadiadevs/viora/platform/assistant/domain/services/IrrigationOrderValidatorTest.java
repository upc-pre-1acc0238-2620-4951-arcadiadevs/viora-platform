package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Answer text validator: irrigation is never ordered, even when the facts allow advising it")
class IrrigationOrderValidatorTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.forLanguageTag("en");
    private static final Map<String, Object> ADVISABLE = Map.of("soilMoisture", 14.0, "criticalMoisture", 16.0,
            AnswerTextValidator.IRRIGATION_ADVISABLE, true, AnswerTextValidator.PRODUCER_DECIDES_IRRIGATION, true);

    @Test
    @DisplayName("An order to irrigate is rejected in Spanish and English")
    void rejectsOrders() {
        assertThat(AnswerTextValidator.check("Riega hoy temprano: tu sonda marca 14 %.", ES, ADVISABLE))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Tu sonda marca 14 %, así que debes regar esta semana.", ES,
                ADVISABLE)).contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Tu sonda marca 14 %: tienes que regar ya.", ES, ADVISABLE))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Your probe reads 14 %, so you should irrigate today.", EN, ADVISABLE))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Irrigate early: your probe reads 14 %.", EN, ADVISABLE))
                .contains(TextRejection.FORBIDDEN_PHRASE);
    }

    @Test
    @DisplayName("Describing the reading and leaving the decision to the producer is accepted when advisable")
    void acceptsDescriptions() {
        assertThat(AnswerTextValidator.check("Tu sonda marca 14 %, bajo el crítico de 16 %. Valora el riego con tu "
                + "técnico.", ES, ADVISABLE)).isEmpty();
        assertThat(AnswerTextValidator.check("Your probe reads 14 %, below the critical 16 %. Weigh irrigation with "
                + "your technician.", EN, ADVISABLE)).isEmpty();
    }
}
