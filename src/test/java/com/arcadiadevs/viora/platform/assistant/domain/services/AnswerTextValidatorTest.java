package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerTextValidator: what a model text must meet before it is shown")
class AnswerTextValidatorTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    private static final Map<String, Object> FACTS = Map.of(
            "preThinningLoad", 0.57, "targetLoad", 0.4, "percentToRemove", 30,
            "irrigationAdvisable", false, "allowedActions", List.of("Registrar aclareo"));

    @Test
    @DisplayName("Accepts a short text whose numbers are all facts, in either decimal style of its language")
    void acceptsABackedText() {
        assertThat(AnswerTextValidator.check(
                "Tu carga está en 0,57 frutos por brote y el objetivo es 0,40. Por eso el plan quita el 30 %.",
                ES, FACTS)).isEmpty();
        assertThat(AnswerTextValidator.check(
                "Your load is 0.57 fruits per shoot and the target is 0.40, so the plan removes 30 %.",
                Locale.ENGLISH, FACTS)).isEmpty();
    }

    @Test
    @DisplayName("Rejects a number that is not a fact, also a rounded one")
    void rejectsAnInventedNumber() {
        assertThat(AnswerTextValidator.check("Tu carga está en 0,6 frutos por brote.", ES, FACTS))
                .contains(TextRejection.UNBACKED_NUMBER);
        assertThat(AnswerTextValidator.check("Quitarás el 35 % de los frutos.", ES, FACTS))
                .contains(TextRejection.UNBACKED_NUMBER);
    }

    @Test
    @DisplayName("Rejects a blank text")
    void rejectsABlankText() {
        assertThat(AnswerTextValidator.check("  ", ES, FACTS)).contains(TextRejection.BLANK);
        assertThat(AnswerTextValidator.check(null, ES, FACTS)).contains(TextRejection.BLANK);
    }

    @Test
    @DisplayName("Rejects more than 280 characters and more than 3 sentences")
    void rejectsLongTexts() {
        assertThat(AnswerTextValidator.check("Tu carga está alta. ".repeat(15).trim(), ES, FACTS))
                .contains(TextRejection.TOO_LONG);
        assertThat(AnswerTextValidator.check("Uno. Dos. Tres. Cuatro.", ES, FACTS))
                .contains(TextRejection.TOO_MANY_SENTENCES);
        assertThat(AnswerTextValidator.check("Uno. Dos. Tres.", ES, FACTS)).isEmpty();
    }

    @Test
    @DisplayName("Rejects irrigation advice unless a fact says irrigation is advisable")
    void rejectsIrrigationAdvice() {
        assertThat(AnswerTextValidator.check("Riega el lote esta semana.", ES, FACTS))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Conviene regar antes del aclareo.", ES, FACTS))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("No se recomienda irrigar.", ES, FACTS))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Water the plot before thinning.", Locale.ENGLISH, FACTS))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Plan your irrigation now.", Locale.ENGLISH, Map.of()))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Riega el lote esta semana.", ES, Map.of("irrigationAdvisable", true)))
                .isEmpty();
        // "riesgo" is not irrigation
        assertThat(AnswerTextValidator.check("Hay riesgo de un año bajo.", ES, FACTS)).isEmpty();
    }

    @Test
    @DisplayName("Rejects recording the thinning when that action is not offered")
    void rejectsAnActionNotOffered() {
        var withoutAction = Map.<String, Object>of("allowedActions", List.of());
        assertThat(AnswerTextValidator.check("Registra el aclareo cuando termines.", ES, withoutAction))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Record the thinning when you finish.", Locale.ENGLISH, withoutAction))
                .contains(TextRejection.FORBIDDEN_PHRASE);
        assertThat(AnswerTextValidator.check("Registra el aclareo cuando termines.", ES, FACTS)).isEmpty();
    }
}
