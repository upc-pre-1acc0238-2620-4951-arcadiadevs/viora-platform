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
                    new DeepLinkProperties.Android("pe.edu.upc.viora", List.of("AB:CD")))))
            .addPlaceholderValue("viora.deep-link.password-reset-path", "/password-resets")
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
    @DisplayName("Something that is not a reset token answers 404")
    void rejectsWhatIsNotAToken() throws Exception {
        mvc.perform(get("/password-resets/short")).andExpect(status().isNotFound());
        mvc.perform(get("/password-resets/" + "x".repeat(20) + "<script>")).andExpect(status().isNotFound());
    }
}
