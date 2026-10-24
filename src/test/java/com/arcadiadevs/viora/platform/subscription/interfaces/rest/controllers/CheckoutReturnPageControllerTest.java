package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Page Mercado Pago returns the producer to")
class CheckoutReturnPageControllerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CheckoutReturnPageController(
            new DeepLinkProperties("https://viora.example", "/password-resets",
                    new DeepLinkProperties.Android("pe.edu.upc.viora", List.of("AB:CD")))))
            .build();

    @Test
    @DisplayName("Opens the app with an explicit intent and ignores what the provider puts in the query")
    void opensTheApp() throws Exception {
        mvc.perform(get("/payments/return?collection_status=approved&external_reference=<script>"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString(
                        "intent://viora.example/payments/return#Intent;scheme=https;package=pe.edu.upc.viora;end")))
                .andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(not(containsString("approved"))));
    }

    @Test
    @DisplayName("Opens the app on the extension result for the payment of a plan extension")
    void opensTheExtensionResult() throws Exception {
        mvc.perform(get("/payments/return/extension?collection_status=approved"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "intent://viora.example/payments/return/extension#Intent;scheme=https;package=pe.edu.upc.viora;end")));
    }
}
