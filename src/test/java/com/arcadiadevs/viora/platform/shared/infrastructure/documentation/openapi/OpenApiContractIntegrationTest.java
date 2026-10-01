package com.arcadiadevs.viora.platform.shared.infrastructure.documentation.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class OpenApiContractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void apiDocsEndpointExposesOpenApiContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Viora Agronomic Platform API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.info.description").value("REST API for Viora agronomic monitoring and management platform"))
                .andExpect(jsonPath("$.info.contact.name").value("Viora Engineering Support"))
                .andExpect(jsonPath("$.info.contact.email").value("support@viora.com"))
                .andExpect(jsonPath("$.info.contact.url").value("https://viora.com/support"))
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"))
                .andExpect(jsonPath("$.info.license.url").value("https://www.apache.org/licenses/LICENSE-2.0.html"))
                .andExpect(jsonPath("$.externalDocs.description").value("Viora Platform Documentation"))
                .andExpect(jsonPath("$.externalDocs.url").value("https://docs.viora.com"))
                .andExpect(jsonPath("$.servers", hasSize(3)))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.servers[0].description").value("Default / Current Server"))
                .andExpect(jsonPath("$.servers[1].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.servers[1].description").value("Local Development Server"))
                .andExpect(jsonPath("$.servers[2].url").value("https://api.viora.com"))
                .andExpect(jsonPath("$.servers[2].description").value("Production Server"));
    }

    @Test
    void swaggerUiEndpointsAreAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
