package com.arcadiadevs.viora.platform.orchard.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controller, services and H2 persistence: the notes a producer writes in the logbook (P75). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fieldnotes;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Field notes of the logbook (P75)")
class FieldNoteControllerIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ProducerTimeZone.ZONE);
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private final String producer = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        authenticateAs(producer);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Stores a note once even when the phone sends it twice")
    void storesANoteOnce() throws Exception {
        var plotId = createPlot();
        var noteId = UUID.randomUUID().toString();

        write(plotId, noteId, "IRRIGATION", "Adelanté el riego a la tarde.", TODAY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.type").value("IRRIGATION"))
                .andExpect(jsonPath("$.text").value("Adelanté el riego a la tarde."))
                .andExpect(jsonPath("$.observedOn").value(TODAY.toString()))
                .andExpect(jsonPath("$.hasPhoto").value(false));
        write(plotId, noteId, "IRRIGATION", "Adelanté el riego a la tarde.", TODAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId));
        write(plotId, noteId, "PRUNING", "Otra cosa.", TODAY).andExpect(status().isConflict());

        assertEquals(1, jdbc.queryForObject("select count(*) from field_notes where plot_id = ?", Integer.class, plotId));
    }

    @Test
    @DisplayName("Lists the notes of a plot, newest day first")
    void listsNewestFirst() throws Exception {
        var plotId = createPlot();
        mvc.perform(get(route(plotId))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        write(plotId, UUID.randomUUID().toString(), "PRUNING", "Poda de formación.", TODAY.minusDays(5))
                .andExpect(status().isCreated());
        write(plotId, UUID.randomUUID().toString(), "PESTS", "Mosca del olivo en la fila 2.", TODAY)
                .andExpect(status().isCreated());

        mvc.perform(get(route(plotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("PESTS"))
                .andExpect(jsonPath("$[1].type").value("PRUNING"));
    }

    @Test
    @DisplayName("Attaches a photo and serves its exact bytes")
    void attachesAPhoto() throws Exception {
        var plotId = createPlot();
        var noteId = UUID.randomUUID().toString();
        write(plotId, noteId, "OTHER", "Foto del daño.", TODAY).andExpect(status().isCreated());
        mvc.perform(get(route(plotId) + "/" + noteId + "/photo")).andExpect(status().isNotFound());

        mvc.perform(put(route(plotId) + "/" + noteId + "/photo").contentType(MediaType.IMAGE_JPEG).content(JPEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPhoto").value(true));
        // A retry replaces the photo instead of failing
        mvc.perform(put(route(plotId) + "/" + noteId + "/photo").contentType(MediaType.IMAGE_JPEG).content(JPEG))
                .andExpect(status().isOk());

        var bytes = mvc.perform(get(route(plotId) + "/" + noteId + "/photo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(JPEG, bytes);
        mvc.perform(get(route(plotId))).andExpect(jsonPath("$[0].hasPhoto").value(true));

        mvc.perform(put(route(plotId) + "/" + noteId + "/photo").contentType(MediaType.IMAGE_PNG)
                        .content("not an image".getBytes()))
                .andExpect(status().isBadRequest());
        mvc.perform(put(route(plotId) + "/" + UUID.randomUUID() + "/photo").contentType(MediaType.IMAGE_JPEG)
                        .content(JPEG))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Rejects invalid notes, unknown plots and other producers' plots")
    void rejectsWhatCannotBeWritten() throws Exception {
        var plotId = createPlot();
        write(plotId, UUID.randomUUID().toString(), "IRRIGATION", "Mañana.", TODAY.plusDays(1))
                .andExpect(status().isBadRequest());
        write(plotId, UUID.randomUUID().toString(), "RIEGO", "Tipo desconocido.", TODAY)
                .andExpect(status().isBadRequest());
        write(plotId, UUID.randomUUID().toString(), "OTHER", "a".repeat(501), TODAY)
                .andExpect(status().isBadRequest());
        write(plotId, "not-a-uuid", "OTHER", "Id inválido.", TODAY).andExpect(status().isBadRequest());
        write(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "OTHER", "Sin lote.", TODAY)
                .andExpect(status().isNotFound());

        authenticateAs(UUID.randomUUID().toString());
        write(plotId, UUID.randomUUID().toString(), "OTHER", "Lote ajeno.", TODAY).andExpect(status().isForbidden());
        mvc.perform(get(route(plotId))).andExpect(status().isForbidden());
    }

    private ResultActions write(String plotId, String noteId, String type, String text, LocalDate observedOn)
            throws Exception {
        var payload = """
                {"id":"%s","type":"%s","text":"%s","observedOn":"%s"}
                """.formatted(noteId, type, text, observedOn);
        return mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON).content(payload));
    }

    private static String route(String plotId) {
        return "/api/v1/plots/" + plotId + "/field-notes";
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private String createPlot() throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"SEVILLANA",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
