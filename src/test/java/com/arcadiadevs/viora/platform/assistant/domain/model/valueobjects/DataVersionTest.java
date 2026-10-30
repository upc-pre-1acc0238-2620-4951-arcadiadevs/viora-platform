package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataVersion")
class DataVersionTest {

    @Test
    @DisplayName("Is sha256 of the facts with sorted keys, whatever their insertion order or number scale")
    void hashesTheCanonicalFacts() {
        var first = new LinkedHashMap<String, Object>();
        first.put("target", 0.4);
        first.put("percent", 30);
        var second = new LinkedHashMap<String, Object>();
        second.put("percent", 30.0);
        second.put("target", 0.40);

        assertThat(DataVersion.canonical(first)).isEqualTo("{\"percent\":30,\"target\":0.4}");
        assertThat(DataVersion.of(first)).isEqualTo(DataVersion.of(second));
        assertThat(DataVersion.of(first).value()).matches("sha256:[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Changes when a fact changes")
    void changesWithTheFacts() {
        assertThat(DataVersion.of(Map.of("percent", 30))).isNotEqualTo(DataVersion.of(Map.of("percent", 20)));
        assertThat(DataVersion.of(Map.of("window", "OPEN"))).isNotEqualTo(DataVersion.of(Map.of("window", "CLOSED")));
    }
}
