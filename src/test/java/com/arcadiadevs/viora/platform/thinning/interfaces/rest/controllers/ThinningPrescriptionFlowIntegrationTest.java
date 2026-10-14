package com.arcadiadevs.viora.platform.thinning.interfaces.rest.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole path of a prescription over the real application: sampling, full bloom, technical profile and
 * the prescription that comes out of them. The profile values are synthetic and exist only in this test.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:thinningflow;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.thinning.profiles.sevillana.target-fruits-per-shoot=0.4",
        "viora.thinning.profiles.sevillana.window-opens-days-after-bloom=14",
        "viora.thinning.profiles.sevillana.window-closes-days-after-bloom=49",
        "viora.thinning.profiles.sevillana.status=SYNTHETIC_DEMO",
        "viora.thinning.profiles.sevillana.version=test-1",
        "viora.thinning.profiles.sevillana.source=integration test",
        "viora.thinning.profiles.sevillana.approved-by=tests"
})
@DisplayName("Thinning prescription: from sampling to an issued prescription")
class ThinningPrescriptionFlowIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningProfiles profiles;

    @Test
    void unavailableProfileRejectsCorrectionAndPreservesPersistedWindow() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom))).andExpect(status().isOk());
        var originalProfiles = profiles.getProfiles();
        try {
            profiles.setProfiles(java.util.Map.of());
            mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                    .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom.plusDays(5))))
                    .andExpect(status().isConflict());
            mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullBloomOn").value(bloom.toString()))
                    .andExpect(jsonPath("$.windowOpensOn").value(bloom.plusDays(14).toString()))
                    .andExpect(jsonPath("$.windowClosesOn").value(bloom.plusDays(49).toString()))
                    .andExpect(jsonPath("$.status").value("PRESCRIBED"));
        } finally {
            profiles.setProfiles(originalProfiles);
        }
    }

    private MockMvc mvc;
    private final int campaign = Year.now().getValue();
    private final LocalDate bloom = LocalDate.of(campaign, 1, 1);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
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

    /** Five trees, 12 fruits on 20 shoots each: 0.6 fruits per shoot. */
    private void sampleFiveTrees(String plotId) throws Exception {
        var samples = new StringBuilder();
        for (int tree = 1; tree <= 5; tree++) {
            samples.append("""
                    {"treeTag":"T-%d","shootCount":20,"fruitSetCount":12,"trunkDiameterMm":150.0,"samplingDate":"%s"}%s
                    """.formatted(tree, LocalDate.now(), tree < 5 ? "," : ""));
        }
        var body = """
                {"clientBatchId":"%s","campaignYear":%d,"samples":[%s]}
                """.formatted(UUID.randomUUID(), campaign, samples);
        mvc.perform(post("/api/v1/plots/{id}/samplings", plotId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isRepresentative").value(true))
                .andExpect(jsonPath("$.meanFruitsPerShoot").value(0.6))
                .andExpect(jsonPath("$.loadUnit").value("FRUITS_PER_SHOOT"));
    }

    private String fullBloom(LocalDate date) {
        return "{\"observedOn\":\"%s\"}".formatted(date);
    }

    @Test
    @DisplayName("There is no prescription before any sampling")
    void nothingBeforeSampling() throws Exception {
        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", createPlot("SEVILLANA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sampled but without full bloom: the prescription waits and says what it needs")
    void waitsForTheFullBloom() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAMPLING_IN_PROGRESS"))
                .andExpect(jsonPath("$.blockers", contains("FULL_BLOOM_MISSING")))
                .andExpect(jsonPath("$.percentageToRemove").doesNotExist())
                .andExpect(jsonPath("$.loadUnit").value("FRUITS_PER_SHOOT"));
    }

    @Test
    @DisplayName("Recording the full bloom issues the prescription with the profile window and provenance")
    void fullBloomIssuesThePrescription() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);

        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRESCRIBED"))
                .andExpect(jsonPath("$.fullBloomOn").value(bloom.toString()))
                .andExpect(jsonPath("$.windowOpensOn").value(bloom.plusDays(14).toString()))
                .andExpect(jsonPath("$.windowClosesOn").value(bloom.plusDays(49).toString()))
                .andExpect(jsonPath("$.windowBasis").value("FULL_BLOOM_PLUS_PROFILE_OFFSETS"))
                .andExpect(jsonPath("$.targetFruitsPerShoot").value(0.4))
                .andExpect(jsonPath("$.percentageToRemove").value(33.33))
                .andExpect(jsonPath("$.profileVersion").value("test-1"))
                .andExpect(jsonPath("$.profileStatus").value("SYNTHETIC_DEMO"))
                .andExpect(jsonPath("$.blockers", empty()));

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRESCRIBED"));
    }

    @Test
    @DisplayName("Sampling after the full bloom issues the prescription as soon as the sampling is representative")
    void samplingIssuesWhenBloomIsKnown() throws Exception {
        var plotId = createPlot("SEVILLANA");
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAMPLING_IN_PROGRESS"))
                .andExpect(jsonPath("$.blockers", contains("SAMPLING_NOT_REPRESENTATIVE")));

        sampleFiveTrees(plotId);

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                .andExpect(jsonPath("$.status").value("PRESCRIBED"))
                .andExpect(jsonPath("$.windowOpensOn").value(bloom.plusDays(14).toString()));
    }

    @Test
    @DisplayName("Correcting the full bloom moves the window of the issued prescription")
    void correctingTheFullBloomMovesTheWindow() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom))).andExpect(status().isOk());

        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom.plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowOpensOn").value(bloom.plusDays(19).toString()))
                .andExpect(jsonPath("$.windowClosesOn").value(bloom.plusDays(54).toString()));
    }

    @Test
    @DisplayName("A variety without a technical profile gets no prescription and is told so")
    void varietyWithoutProfileGetsNone() throws Exception {
        var plotId = createPlot("CRIOLLA");
        sampleFiveTrees(plotId);

        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAMPLING_IN_PROGRESS"))
                .andExpect(jsonPath("$.blockers", contains("TARGET_NOT_CONFIGURED")))
                .andExpect(jsonPath("$.percentageToRemove").doesNotExist());
    }

    @Test
    @DisplayName("Rejects a future full bloom, one outside the campaign and an unknown plot")
    void rejectsInvalidFullBloom() throws Exception {
        var plotId = createPlot("SEVILLANA");

        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(2))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignYear\":%d,\"observedOn\":\"%s\"}".formatted(campaign, bloom.minusYears(1))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The status filter still works: ACTIVE shows only the issued prescription")
    void activeFilterShowsOnlyTheIssuedOne() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId).param("status", "ACTIVE"))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom))).andExpect(status().isOk());

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockers", empty()));
        assertEquals(campaign, Year.now().getValue());
    }

    private void recordHarvest(String plotId, int year, double kg) throws Exception {
        var body = """
                {"campaignYear":%d,"totalYieldKg":%s,"greenKg":%s,"blackKg":0.0}
                """.formatted(year, kg, kg);
        mvc.perform(post("/api/v1/plots/{id}/harvest-records", plotId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private double areaOf(String plotId) throws Exception {
        var response = mvc.perform(get("/api/v1/plots/{id}", plotId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.areaHa")).doubleValue();
    }

    @Test
    @DisplayName("US26: before thinning the plan shows the load against the target, the sampling and the profile offsets")
    void outlookShowsTheLoadBeforeThinning() throws Exception {
        var plotId = createPlot("SEVILLANA");
        sampleFiveTrees(plotId);

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLoad.fruitsPerShoot").value(0.6))
                .andExpect(jsonPath("$.currentLoad.targetFruitsPerShoot").value(0.4))
                .andExpect(jsonPath("$.currentLoad.loadRatio").value(1.5))
                .andExpect(jsonPath("$.currentLoad.loadState").value("SEVERE_OVERLOAD"))
                .andExpect(jsonPath("$.sampledTrees").value(5))
                .andExpect(jsonPath("$.treesNeeded").value(0))
                .andExpect(jsonPath("$.lastSampledOn").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.windowOpensDaysAfterBloom").value(14))
                .andExpect(jsonPath("$.windowClosesDaysAfterBloom").value(49))
                .andExpect(jsonPath("$.yieldEstimate.status").value("INSUFFICIENT_HISTORY"))
                .andExpect(jsonPath("$.yieldEstimate.harvestCampaigns").value(0))
                .andExpect(jsonPath("$.yieldEstimate.requiredHarvestCampaigns").value(3))
                .andExpect(jsonPath("$.yieldEstimate.sustainableKgPerHa").doesNotExist());
    }

    @Test
    @DisplayName("US26: without a representative sampling or a profile there is no current load")
    void noCurrentLoadWithoutSamplingOrProfile() throws Exception {
        var unsampled = createPlot("SEVILLANA");
        mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", unsampled)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLoad").doesNotExist())
                .andExpect(jsonPath("$.sampledTrees").value(0))
                .andExpect(jsonPath("$.treesNeeded").value(5));

        var withoutProfile = createPlot("CRIOLLA");
        sampleFiveTrees(withoutProfile);
        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", withoutProfile))
                .andExpect(jsonPath("$.currentLoad").doesNotExist())
                .andExpect(jsonPath("$.windowOpensDaysAfterBloom").doesNotExist());
    }

    @Test
    @DisplayName("US26: with three earlier harvests the yield is estimated from the plot history and the load")
    void yieldComesFromTheHarvestHistory() throws Exception {
        var plotId = createPlot("SEVILLANA");
        recordHarvest(plotId, campaign - 3, 9000.0);
        recordHarvest(plotId, campaign - 2, 7000.0);
        recordHarvest(plotId, campaign - 1, 8000.0);
        sampleFiveTrees(plotId);
        double sustainable = 8000.0 / areaOf(plotId);

        mvc.perform(get("/api/v1/plots/{id}/thinning-prescriptions", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yieldEstimate.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.yieldEstimate.harvestCampaigns").value(3))
                .andExpect(jsonPath("$.yieldEstimate.sustainableKgPerHa", closeTo(sustainable, 0.06)))
                .andExpect(jsonPath("$.yieldEstimate.estimatedKgPerHa", closeTo(sustainable * 1.5, 0.06)))
                .andExpect(jsonPath("$.yieldEstimate.basis").value("FRUIT_COUNT_ONLY"));
    }

    @Test
    @DisplayName("P63: the confirmation estimates the yield with the load left by the labor")
    void confirmationEstimatesTheYieldAfterThinning() throws Exception {
        var plotId = createPlot("SEVILLANA");
        recordHarvest(plotId, campaign - 3, 9000.0);
        recordHarvest(plotId, campaign - 2, 7000.0);
        recordHarvest(plotId, campaign - 1, 8000.0);
        sampleFiveTrees(plotId);
        var issued = mvc.perform(put("/api/v1/plots/{id}/thinning-prescriptions/full-bloom", plotId)
                        .contentType(MediaType.APPLICATION_JSON).content(fullBloom(bloom)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(issued, "$.id");
        double sustainable = 8000.0 / areaOf(plotId);
        var body = """
                {"executedDate":"%s","actualRemovalPercentage":50.0,"removedKg":100.0,"laborCrewSize":2}
                """.formatted(LocalDate.now(java.time.ZoneOffset.UTC));

        mvc.perform(post("/api/v1/thinning-prescriptions/{id}/execution-confirmations", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loadBalance.residualFruitsPerShoot").value(0.3))
                .andExpect(jsonPath("$.yieldEstimate.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.yieldEstimate.estimatedKgPerHa", closeTo(sustainable * 0.75, 0.06)))
                .andExpect(jsonPath("$.yieldEstimate.basis").value("FRUIT_COUNT_ONLY"));
    }
}
