package com.arcadiadevs.viora.platform.settlement.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controllers, services, PDF adapter and H2 persistence: the dossier a producer seals (US30) end to end. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sealeddossier;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Agronomic dossier sealed by the producer (US30)")
class AgronomicDossierControllerIntegrationTest {

    /** Weighing date the settlements carry: yesterday, so it is never in the future whenever the suite runs. */
    private static final LocalDate WEIGHED_ON = LocalDate.now(ZoneOffset.UTC).minusDays(1);

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    private final CryptographicHashService hashService = new CryptographicHashService();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of())
        );
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Seals the first settled campaign and serves the exact PDF its SHA-256 belongs to")
    void sealsTheFirstSettledCampaign() throws Exception {
        var plotId = createPlot("SEVILLANA");
        recordHistory(plotId, 2023, 9000.0, 3000.0, 8500.0);
        settle(plotId, 2026, 300, 100).andExpect(status().isCreated());

        var response = seal(plotId, 2026)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.campaignYear").value(2026))
                .andExpect(jsonPath("$.variety").value("SEVILLANA"))
                .andExpect(jsonPath("$.harvest.totalKg").value(400.0))
                .andExpect(jsonPath("$.harvest.greenKg").value(300.0))
                .andExpect(jsonPath("$.harvest.receiptNumber").isNotEmpty())
                .andExpect(jsonPath("$.history.fromYear").value(2023))
                .andExpect(jsonPath("$.history.toYear").value(2026))
                .andExpect(jsonPath("$.history.campaigns").value(4))
                .andExpect(jsonPath("$.thinning.compliance").value("NOT_RECORDED"))
                .andExpect(jsonPath("$.fruitLoad.loadUnit").value("FRUITS_PER_SHOOT"))
                .andExpect(jsonPath("$.fruitLoad.preThinningFruitsPerShoot").doesNotExist())
                .andExpect(jsonPath("$.climate.linkedNodes").isNumber())
                .andExpect(jsonPath("$.pageCount").isNumber())
                .andExpect(jsonPath("$.fileName", containsString("_2026.pdf")))
                .andExpect(jsonPath("$.certification").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String hash = JsonPath.read(response, "$.verificationHash");
        assertTrue(hash.matches("[0-9a-f]{64}"));

        var pdf = mvc.perform(get(route(plotId) + "/2026").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("X-Verification-Hash", hash))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(hash, hashService.sha256(pdf).value());
        assertEquals(((Number) JsonPath.read(response, "$.sizeBytes")).intValue(), pdf.length);

        var text = pdfText(pdf);
        assertTrue(text.contains("Expediente agronómico"), text);
        assertTrue(text.contains("Historial productivo"));
        assertTrue(text.contains("2026 (asentada)"));
        assertTrue(text.contains("9 000 kg"));
        assertTrue(text.contains("Sin aclareo registrado"));
        assertTrue(text.contains("Aún no se puede medir la estabilización"));
        assertFalse(text.contains(hash));

        mvc.perform(get(route(plotId) + "/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationHash").value(hash));
    }

    @Test
    @DisplayName("Sealing again returns the stored dossier unchanged")
    void sealingAgainReturnsTheStoredDossier() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        var first = seal(plotId, 2026).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        seal(plotId, 2026)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((String) JsonPath.read(first, "$.id")))
                .andExpect(jsonPath("$.verificationHash").value((String) JsonPath.read(first, "$.verificationHash")));
        assertEquals(1, jdbc.queryForObject("select count(*) from sealed_dossiers where plot_id = ?", Integer.class,
                plotId));
    }

    @Test
    @DisplayName("Anyone holding the PDF can verify it by its code")
    void verifiesByTheCode() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        String hash = JsonPath.read(seal(plotId, 2026).andReturn().getResponse().getContentAsString(),
                "$.verificationHash");

        mvc.perform(get("/api/v1/agronomic-dossiers/verifications/{hash}", hash.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").value(true))
                .andExpect(jsonPath("$.plotId").value(plotId))
                .andExpect(jsonPath("$.campaignYear").value(2026))
                .andExpect(jsonPath("$.pageCount").isNumber());
        mvc.perform(get("/api/v1/agronomic-dossiers/verifications/{hash}", "0".repeat(64)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/agronomic-dossiers/verifications/{hash}", "not-a-hash"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lists the dossiers of a plot, most recent campaign first")
    void listsTheDossiersOfAPlot() throws Exception {
        var plotId = createPlot("CRIOLLA");
        mvc.perform(get(route(plotId))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        settle(plotId, 2027, 200, 100).andExpect(status().isCreated());
        seal(plotId, 2026).andExpect(status().isCreated());
        seal(plotId, 2027).andExpect(status().isCreated());

        mvc.perform(get(route(plotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].campaignYear").value(2027))
                .andExpect(jsonPath("$[1].campaignYear").value(2026));
    }

    @Test
    @DisplayName("Shows the agronomist certification of the same campaign when there is one")
    void showsTheAgronomistCertification() throws Exception {
        var plotId = createPlot("CRIOLLA");
        settle(plotId, 2026, 100, 100).andExpect(status().isCreated());
        settle(plotId, 2027, 300, 100).andExpect(status().isCreated());
        settle(plotId, 2028, 150, 100).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/plots/{id}/certifications", plotId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":2028,\"auditorSignature\":\"CIP-49120\",\"certifiedBy\":\"Ing. Sanchez\","
                                + "\"cipNumber\":\"49120\"}"))
                .andExpect(status().isCreated());
        seal(plotId, 2028).andExpect(status().isCreated());

        mvc.perform(get(route(plotId) + "/2028"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certification.certifiedBy").value("Ing. Sanchez"))
                .andExpect(jsonPath("$.certification.cipNumber").value("49120"));
    }

    @Test
    @DisplayName("Rejects an unsettled campaign, an unknown plot and invalid input")
    void rejectsWhatCannotBeSealed() throws Exception {
        var plotId = createPlot("CRIOLLA");
        seal(plotId, 2026).andExpect(status().isUnprocessableEntity());
        seal(UUID.randomUUID().toString(), 2026).andExpect(status().isNotFound());
        seal("not-a-uuid", 2026).andExpect(status().isBadRequest());
        seal(plotId, 1990).andExpect(status().isBadRequest());
        mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(route(plotId) + "/2026")).andExpect(status().isNotFound());
        mvc.perform(get(route(plotId) + "/2026").accept(MediaType.APPLICATION_PDF)).andExpect(status().isNotFound());
    }

    private ResultActions seal(String plotId, int year) throws Exception {
        return mvc.perform(post(route(plotId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"campaignYear\":%d}".formatted(year)));
    }

    private static String route(String plotId) {
        return "/api/v1/plots/" + plotId + "/agronomic-dossiers";
    }

    private ResultActions settle(String plotId, int year, double green, double black) throws Exception {
        return mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campaignYear\":%d,\"greenOlivesKg\":%s,\"blackOlivesKg\":%s,\"weighedOn\":\"%s\"}"
                        .formatted(year, green, black, WEIGHED_ON)));
    }

    private void recordHistory(String plotId, int firstYear, double... kilograms) throws Exception {
        for (int i = 0; i < kilograms.length; i++) {
            mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"campaignYear\":%d,\"totalYieldKg\":%s}".formatted(firstYear + i, kilograms[i])))
                    .andExpect(status().isCreated());
        }
    }

    private String createPlot(String variety) throws Exception {
        var payload = """
                {"name":"Cuartel %s","variety":"%s",
                 "polygonGeoJson":"{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                 "rowSpacingM":7.0,"treeSpacingM":5.0}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), variety);
        var response = mvc.perform(post("/api/v1/plots").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private static String pdfText(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }
}
