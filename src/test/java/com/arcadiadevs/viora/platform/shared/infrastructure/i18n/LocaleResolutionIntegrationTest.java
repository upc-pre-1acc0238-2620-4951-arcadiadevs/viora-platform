package com.arcadiadevs.viora.platform.shared.infrastructure.i18n;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LocaleResolutionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LocaleResolver localeResolver;

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/v1/test-i18n")
    static class TestI18nController {

        @GetMapping("/unexpected")
        public String triggerUnexpected() {
            throw new RuntimeException("internal-error");
        }

        @GetMapping("/not-found")
        public String triggerNotFound() {
            throw new ResourceNotFoundException("Entity", 42L);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void whenNoAcceptLanguageHeader_resolvesDefaultEnglishMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-i18n/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void whenAcceptLanguageEnglish_resolvesEnglishMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-i18n/unexpected")
                        .header("Accept-Language", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void whenAcceptLanguageSpanish_resolvesSpanishMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-i18n/unexpected")
                        .header("Accept-Language", "es"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrio un error inesperado."));
    }

    @Test
    void whenAcceptLanguageUnsupported_fallsBackToDefaultEnglishMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-i18n/unexpected")
                        .header("Accept-Language", "fr"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
