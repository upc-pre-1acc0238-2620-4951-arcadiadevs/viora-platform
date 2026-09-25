package com.arcadiadevs.viora.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class VioraPlatformApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testCreatePlotPayload() throws Exception {
        String payload = """
                {
                  "producerId": "550e8400-e29b-41d4-a716-446655440000",
                  "name": "Cuartel San Jerónimo",
                  "variety": "CRIOLLA",
                  "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                  "rowSpacingM": 7,
                  "treeSpacingM": 5
                }
                """;

        mockMvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void testListPlotsEndpoint() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/plots"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}

