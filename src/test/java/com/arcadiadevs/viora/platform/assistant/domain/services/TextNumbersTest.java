package com.arcadiadevs.viora.platform.assistant.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextNumbers: numbers of a text against its facts")
class TextNumbersTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    @Test
    @DisplayName("Reads Spanish decimals and thousands, and English ones")
    void readsNumbersByLocale() {
        assertThat(TextNumbers.extract("Tu carga está en 0,57 frutos y cosechaste 3.000 kg en 2026.", ES))
                .containsExactly(new BigDecimal("0.57"), new BigDecimal("3000"), new BigDecimal("2026"));
        assertThat(TextNumbers.extract("Your load is 0.57 fruits and you harvested 3,000 kg in 2026.", Locale.ENGLISH))
                .containsExactly(new BigDecimal("0.57"), new BigDecimal("3000"), new BigDecimal("2026"));
        assertThat(TextNumbers.extract("Quitar el 30 % de los frutos (−30 %).", ES))
                .containsExactly(new BigDecimal("30"), new BigDecimal("30"));
    }

    @Test
    @DisplayName("Facts give their numbers, also the ones inside dates and grades")
    void readsTheNumbersOfTheFacts() {
        var numbers = TextNumbers.factNumbers(Map.of("load", 0.4, "closesOn", "2026-11-01", "grade", "91/100",
                "issued", true));

        assertThat(numbers).usingElementComparator(BigDecimal::compareTo).contains(
                new BigDecimal("0.4"), new BigDecimal("2026"), new BigDecimal("11"), BigDecimal.ONE,
                new BigDecimal("91"), new BigDecimal("100"));
    }

    @Test
    @DisplayName("A text is backed only when every number is a fact; 0,40 matches 0.4")
    void checksTheTextAgainstTheFacts() {
        var facts = Map.<String, Object>of("load", 0.57, "target", 0.4, "percent", 30, "closesOn", "2026-11-12");

        assertThat(TextNumbers.unbackedNumbers("Carga 0,57 y objetivo 0,40: quita el 30 % antes del 12 de noviembre.",
                ES, facts)).isEmpty();
        assertThat(TextNumbers.unbackedNumbers("Quita 3 de cada 10 frutos.", ES, facts))
                .containsExactly(new BigDecimal("3"), new BigDecimal("10"));
    }
}
