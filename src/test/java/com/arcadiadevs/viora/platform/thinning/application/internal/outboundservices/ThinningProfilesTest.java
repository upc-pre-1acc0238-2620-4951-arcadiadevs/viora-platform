package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThinningProfilesTest {

    private static ThinningProfiles bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("viora.thinning", ThinningProfiles.class)
                .orElseGet(ThinningProfiles::new);
    }

    private static Map<String, String> complete() {
        var properties = new HashMap<String, String>();
        var prefix = "viora.thinning.profiles.sevillana.";
        properties.put(prefix + "target-fruits-per-shoot", "0.4");
        properties.put(prefix + "window-opens-days-after-bloom", "14");
        properties.put(prefix + "window-closes-days-after-bloom", "49");
        properties.put(prefix + "status", "SYNTHETIC_DEMO");
        properties.put(prefix + "version", "demo-1");
        properties.put(prefix + "source", "unit test");
        properties.put(prefix + "approved-by", "tests");
        return properties;
    }

    @Test
    @DisplayName("There is no profile unless one is configured: nothing is invented by default")
    void noDefaults() {
        assertThat(bind(Map.of()).forVariety("SEVILLANA")).isEmpty();
    }

    @Test
    @DisplayName("Reads a complete profile, whatever the case of the variety")
    void readsACompleteProfile() {
        var profile = bind(complete()).forVariety("Sevillana").orElseThrow();

        assertThat(profile.variety()).isEqualTo("SEVILLANA");
        assertThat(profile.targetFruitsPerShoot()).isEqualTo(0.4);
        assertThat(profile.windowOpensDaysAfterBloom()).isEqualTo(14);
        assertThat(profile.windowClosesDaysAfterBloom()).isEqualTo(49);
        assertThat(profile.status()).isEqualTo("SYNTHETIC_DEMO");
        assertThat(profile.version()).isEqualTo("demo-1");
    }

    @Test
    @DisplayName("Ignores a profile that lacks its target, its window or who approved it")
    void ignoresIncompleteProfiles() {
        for (var missing : new String[] {"target-fruits-per-shoot", "window-opens-days-after-bloom",
                "window-closes-days-after-bloom", "status", "version", "source", "approved-by"}) {
            var properties = complete();
            properties.remove("viora.thinning.profiles.sevillana." + missing);

            assertThat(bind(properties).forVariety("SEVILLANA")).as(missing).isEmpty();
        }
    }

    @Test
    @DisplayName("Ignores a profile whose window closes before it opens or whose status is unknown")
    void ignoresIncoherentProfiles() {
        var window = complete();
        window.put("viora.thinning.profiles.sevillana.window-closes-days-after-bloom", "7");
        var status = complete();
        status.put("viora.thinning.profiles.sevillana.status", "MADE_UP");

        assertThat(bind(window).forVariety("SEVILLANA")).isEmpty();
        assertThat(bind(status).forVariety("SEVILLANA")).isEmpty();
    }

    @Test
    @DisplayName("A variety without its own profile gets none")
    void otherVarietiesGetNone() {
        assertThat(bind(complete()).forVariety("CRIOLLA")).isEmpty();
    }
}
