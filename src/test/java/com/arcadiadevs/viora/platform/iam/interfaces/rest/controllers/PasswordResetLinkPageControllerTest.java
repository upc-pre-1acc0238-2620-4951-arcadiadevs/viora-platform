package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PasswordResetLinkPageController Unit Tests")
class PasswordResetLinkPageControllerTest {

    private static final String TOKEN = "Ab9_x-Zq3Lm0Pq8Rs7Tu6Vw5Xy4Za3Bc2De1Fg0Hi9";

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PasswordResetLinkPageController(
            new DeepLinkProperties("https://viora.example", "/password-resets",
                    new DeepLinkProperties.Android("pe.edu.upc.viora", List.of("AB:CD")),
                    new DeepLinkProperties.Gestor("/gestor/password-resets", "viora-gestor",
                            new DeepLinkProperties.Android("pe.edu.upc.viora.gestor", List.of("12:34"))))))
            .addPlaceholderValue("viora.deep-link.password-reset-path", "/password-resets")
            .addPlaceholderValue("viora.deep-link.gestor.password-reset-path", "/gestor/password-resets")
            .build();

    @Test
    @DisplayName("The link opened in a browser answers a page that opens the app with an explicit intent")
    void answersPageThatOpensTheApp() throws Exception {
        mvc.perform(get("/password-resets/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("intent://viora.example/password-resets/" + TOKEN
                        + "#Intent;scheme=https;package=pe.edu.upc.viora;end")))
                .andExpect(content().string(containsString("Abrir en la app")));
    }

    @Test
    @DisplayName("The Gestor link names the Gestor app on Android and its URL scheme on iOS")
    void answersPageThatOpensTheGestorApp() throws Exception {
        mvc.perform(get("/gestor/password-resets/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("intent://viora.example/gestor/password-resets/" + TOKEN
                        + "#Intent;scheme=https;package=pe.edu.upc.viora.gestor;end")))
                .andExpect(content().string(containsString(
                        "data-ios-url=\"viora-gestor://open/gestor/password-resets/" + TOKEN + "\"")));
    }

    @Test
    @DisplayName("The producer link has no URL scheme for iOS")
    void producerPageHasNoIosScheme() throws Exception {
        mvc.perform(get("/password-resets/" + TOKEN))
                .andExpect(content().string(containsString("data-ios-url=\"\"")));
    }

    @Test
    @DisplayName("Something that is not a reset token answers 404")
    void rejectsWhatIsNotAToken() throws Exception {
        mvc.perform(get("/password-resets/short")).andExpect(status().isNotFound());
        mvc.perform(get("/password-resets/" + "x".repeat(20) + "<script>")).andExpect(status().isNotFound());
        mvc.perform(get("/gestor/password-resets/short")).andExpect(status().isNotFound());
    }
}
