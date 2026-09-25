package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OliveVariety Value Object Unit Tests")
class OliveVarietyTest {

    @ParameterizedTest
    @ValueSource(strings = {"CRIOLLA", "criolla", "  Criolla  "})
    @DisplayName("Should successfully parse valid variety regardless of case or whitespace")
    void shouldParseValidVariety(String input) {
        var variety = OliveVariety.from(input);
        assertThat(variety).isEqualTo(OliveVariety.CRIOLLA);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when variety is null")
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> OliveVariety.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.variety.blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Should throw IllegalArgumentException when variety is blank")
    void shouldThrowWhenBlank(String input) {
        assertThatThrownBy(() -> OliveVariety.from(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.variety.blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "PICUAL", "HOJIBLANCA"})
    @DisplayName("Should throw IllegalArgumentException when variety is unrecognized")
    void shouldThrowWhenUnrecognized(String input) {
        assertThatThrownBy(() -> OliveVariety.from(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.variety.unknown");
    }
}
