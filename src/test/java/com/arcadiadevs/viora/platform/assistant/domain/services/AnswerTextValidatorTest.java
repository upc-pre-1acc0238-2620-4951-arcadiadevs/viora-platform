package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerTextValidator: the model only rephrases")
class AnswerTextValidatorTest {

    private static final String TEMPLATE = "Tu carga está en 0,60 frutos por brote y el árbol sostiene 0,40. "
            + "Quitar 30 % de los frutos la deja en el objetivo.";
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final List<AnswerFact> FACTS = List.of(
            new AnswerFact("0,60", "frutos por brote · estimado", "objetivo 0,40", FactTone.PRIMARY),
            new AnswerFact("−30 %", "de los frutos", "ventana hasta el 14 nov", FactTone.HIGHLIGHT));

    @Test
    @DisplayName("a rephrasing with the same figures is accepted")
    void acceptsTheSameFigures() {
        var text = "Hoy tu árbol lleva 0,60 frutos por brote, pero solo sostiene 0,40. "
                + "Si quitas el 30 % de los frutos, llegas justo al objetivo.";

        assertThat(AnswerTextValidator.validate(text, TEMPLATE, FACTS, ES)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    @DisplayName("a figure of the cards may be used even if the template does not write it, in either decimal style")
    void acceptsFiguresOfTheCards() {
        var text = "You carry 0.6 fruits per shoot and the tree holds 0.4; remove 30% before 14 Nov.";

        assertThat(AnswerTextValidator.validate(text, TEMPLATE, FACTS, EN)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    @DisplayName("an invented or recalculated figure keeps the template")
    void rejectsAnInventedFigure() {
        assertThat(AnswerTextValidator.validate("Tu carga es 0,60 y debes quitar el 33 % de los frutos.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.UNKNOWN_NUMBER);
        assertThat(AnswerTextValidator.validate("Quita 30 % y cosecharás 12 t/ha.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.UNKNOWN_NUMBER);
        assertThat(AnswerTextValidator.validate("Tu carga está en 0,6 frutos; el objetivo es 0,45.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.UNKNOWN_NUMBER);
    }

    @Test
    @DisplayName("irrigation, inputs or promises the template does not mention keep the template")
    void rejectsPracticesTheTemplateDoesNotMention() {
        assertThat(AnswerTextValidator.validate("Quita 30 % de los frutos y aumenta el riego.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORBIDDEN_TERM);
        assertThat(AnswerTextValidator.validate("Quita 30 % y aplica un fungicida.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORBIDDEN_TERM);
        assertThat(AnswerTextValidator.validate("Quita 30 % y te garantizo una buena cosecha.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORBIDDEN_TERM);
    }

    @Test
    @DisplayName("a practice the template does mention is allowed")
    void allowsPracticesOfTheTemplate() {
        var template = "Sin sondas reales no puedo recomendar riego: la humedad de 18 % es un dato simulado.";

        assertThat(AnswerTextValidator.validate("La humedad de 18 % es simulada, así que no recomiendo riego.",
                template, List.of(), ES)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    @DisplayName("an empty, long, listed or linked text keeps the template")
    void rejectsTheWrongShape() {
        assertThat(AnswerTextValidator.validate("  ", TEMPLATE, FACTS, ES)).isEqualTo(Verdict.EMPTY);
        assertThat(AnswerTextValidator.validate(null, TEMPLATE, FACTS, ES)).isEqualTo(Verdict.EMPTY);
        assertThat(AnswerTextValidator.validate("Uno. Dos. Tres. Cuatro.", TEMPLATE, FACTS, ES)).isEqualTo(Verdict.TOO_LONG);
        assertThat(AnswerTextValidator.validate("a".repeat(AnswerTextValidator.MAX_CHARACTERS + 1), TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.TOO_LONG);
        assertThat(AnswerTextValidator.validate("- Quita **30 %** de los frutos", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORMAT);
        assertThat(AnswerTextValidator.validate("Quita 30 %.\nLlegas al objetivo.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORMAT);
        assertThat(AnswerTextValidator.validate("Mira https://viora.pe para quitar 30 %.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORMAT);
        // Seen from gpt-oss-120b on 2026-10-30: the sign of the card copied into the sentence
        assertThat(AnswerTextValidator.validate("Quitarle −30 % de los frutos la deja en el objetivo.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORMAT);
        assertThat(AnswerTextValidator.validate("Quita 30 % y quedas un +50 % sobre 0,40.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.FORMAT);
        assertThat(AnswerTextValidator.validate("Entre 2023-2024 quitaste 30 % de los frutos.", "En 2023 y 2024 quitaste 30 %.", List.of(), ES))
                .isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    @DisplayName("an answer in the other language, or with a stray Spanish word in English, keeps the template")
    void rejectsTheWrongLanguage() {
        // Both seen from gpt-oss-120b on 2026-10-30
        assertThat(AnswerTextValidator.validate("Tu índice de alternancia es 0.50, lo que indica alternancia moderada.",
                "Your alternate bearing index is 0.50: moderate alternate bearing.", List.of(), EN))
                .isEqualTo(Verdict.WRONG_LANGUAGE);
        assertThat(AnswerTextValidator.validate("If you remove 30% of the fruit, tú reach the target.",
                "Removing 30% of the fruit brings it to the target.", List.of(), EN))
                .isEqualTo(Verdict.WRONG_LANGUAGE);
        assertThat(AnswerTextValidator.validate("Your load is 0,60 and the tree holds 0,40.", TEMPLATE, FACTS, ES))
                .isEqualTo(Verdict.WRONG_LANGUAGE);
    }

    @Test
    @DisplayName("a tendency stated as a certainty keeps the template")
    void rejectsALostHedge() {
        // Both seen from gpt-oss-120b on 2026-10-30
        var nextYear = "Con tu historial, 2027 tiende a ser un año de carga baja: tus campañas bajas rondaron 4,5 t/ha.";
        assertThat(AnswerTextValidator.validate("Con tu historial, 2027 será un año de carga baja; tus campañas bajas "
                + "rondaron 4,5 t/ha.", nextYear, List.of(), ES)).isEqualTo(Verdict.LOST_HEDGE);
        var ifNone = "Sin aclarear te quedas en 0,60 frutos por brote. Con esa carga se espera una cosecha más baja.";
        assertThat(AnswerTextValidator.validate("Si no aclaras, tendrás 0,60 frutos por brote. Esa carga reduce la "
                + "cosecha.", ifNone, List.of(), ES)).isEqualTo(Verdict.LOST_HEDGE);
        assertThat(AnswerTextValidator.validate("Si no aclaras, tendrás 0,60 frutos por brote y se espera una cosecha "
                + "más baja.", ifNone, List.of(), ES)).isEqualTo(Verdict.ACCEPTED);
        assertThat(AnswerTextValidator.validate("In 2026 the tree will carry more fruit.",
                "In 2026 the tree tends to carry more fruit.", List.of(), EN)).isEqualTo(Verdict.LOST_HEDGE);
    }

    @Test
    @DisplayName("a decimal point inside a number is not the end of a sentence")
    void decimalsAreNotSentences() {
        var text = "You carry 0.60 fruits per shoot. The tree holds 0.40. Remove 30% to reach it.";

        assertThat(AnswerTextValidator.validate(text, TEMPLATE, FACTS, EN)).isEqualTo(Verdict.ACCEPTED);
    }

    @Test
    @DisplayName("thin and no-break spaces the model writes become plain spaces")
    void normalizesSpaces() {
        assertThat(AnswerTextValidator.normalize("  5,0\u202Ft/ha y 30\u00A0% \n"))
                .isEqualTo("5,0 t/ha y 30 %");
        assertThat(AnswerTextValidator.normalize(null)).isNull();
    }

    @Test
    @DisplayName("numbers compare by value, whatever the decimal mark, trailing zeros and sign")
    void numbersCompareByValue() {
        assertThat(AnswerTextValidator.numbers("−30 %, 0,60 y 10,0 → 4,0 en 2027"))
                .extracting(BigDecimal::toPlainString)
                .containsExactlyInAnyOrder("30", "0.6", "10", "4", "2027");
        assertThat(AnswerTextValidator.numbers("10")).isEqualTo(AnswerTextValidator.numbers("10,00"));
    }
}
