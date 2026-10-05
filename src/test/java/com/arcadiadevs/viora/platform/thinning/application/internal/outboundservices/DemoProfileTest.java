package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** The demo profile must load for every variety and must always say that its values are synthetic. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thinningdemo;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("demo")
@DisplayName("Thinning demo profile")
class DemoProfileTest {

    @Autowired
    ThinningProfiles profiles;

    @Test
    @DisplayName("Gives every variety a profile marked as synthetic demo")
    void everyVarietyHasASyntheticProfile() {
        for (var variety : new String[] {"CRIOLLA", "SEVILLANA", "MANZANILLA", "ARBEQUINA"}) {
            var profile = profiles.forVariety(variety).orElseThrow();

            assertThat(profile.status()).as(variety).isEqualTo("SYNTHETIC_DEMO");
            assertThat(profile.version()).as(variety).startsWith("demo");
            assertThat(profile.source()).as(variety).containsIgnoringCase("sinteticos");
        }
    }
}
