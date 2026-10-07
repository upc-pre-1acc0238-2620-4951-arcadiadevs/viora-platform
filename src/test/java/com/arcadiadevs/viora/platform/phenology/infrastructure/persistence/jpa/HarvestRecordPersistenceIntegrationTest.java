package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context persistence tests for US21 covering rectification and removal of harvest records.
 *
 * <p>Unlike {@code HarvestRecordControllerIntegrationTest}, which mocks the command service and therefore
 * only proves the HTTP mapping, these tests run the whole stack against the real H2 database: real
 * controllers, real application services, real JPA repositories. Every scenario asserts the database
 * state with raw SQL so that a passing test proves the change was physically persisted, not merely
 * reflected in the in-memory aggregate.</p>
 *
 * <p>Harvest fixtures model a real biennial bearing series (on year / off year alternation) so that
 * removing a campaign visibly changes both the Hoblyn BBI and the per-record bearing classification.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:harvestrecordpersistence;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Harvest Record Removal and Rectification Persistence Integration Tests (US21)")
class HarvestRecordPersistenceIntegrationTest {

    /**
     * Biennial bearing series recorded for every scenario.
     * 2021=12000kg (on), 2022=4000kg (off), 2023=13000kg (on), 2024=5000kg (off).
     */
    private static final int ON_YEAR_FIRST = 2021;
    private static final int OFF_YEAR_REMOVED = 2022;
    private static final int ON_YEAR_SECOND = 2023;
    private static final int OFF_YEAR_SECOND = 2024;

    private static final double FIRST_ON_KG = 12000.0;
    private static final double REMOVED_OFF_KG = 4000.0;
    private static final double SECOND_ON_KG = 13000.0;
    private static final double SECOND_OFF_KG = 5000.0;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ---------------------------------------------------------------------------------------------
    // US21 acceptance criterion 2 - Delete
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE harvest record removes its row from the database and keeps the other three campaigns")
    void shouldRemoveCampaignFromDatabaseAndKeepRemainingCampaigns() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Harvest record deleted successfully")));

        // The row must be physically gone, not just hidden from the aggregate.
        assertThat(countHarvestRows(plotId)).isEqualTo(3);
        assertThat(campaignYearsInDatabase(plotId)).doesNotContain(OFF_YEAR_REMOVED);

        var records = listHarvestRecords(plotId);
        assertThat(records).containsOnlyKeys(ON_YEAR_FIRST, ON_YEAR_SECOND, OFF_YEAR_SECOND);

        // Classification is recomputed against the new mean of the three surviving campaigns
        // (mean = 10000kg): 12000/10000 = 1.20 -> BALANCED, 13000/10000 = 1.30 -> ON_YEAR,
        // 5000/10000 = 0.50 -> OFF_YEAR. Note 2021 was ON_YEAR while all four campaigns existed.
        assertThat(records.get(ON_YEAR_FIRST).bearingClassification()).isEqualTo("BALANCED");
        assertThat(records.get(ON_YEAR_SECOND).bearingClassification()).isEqualTo("ON_YEAR");
        assertThat(records.get(OFF_YEAR_SECOND).bearingClassification()).isEqualTo("OFF_YEAR");
        assertThat(records.get(ON_YEAR_FIRST).totalYieldKg()).isEqualTo(FIRST_ON_KG);
        assertThat(records.get(ON_YEAR_SECOND).totalYieldKg()).isEqualTo(SECOND_ON_KG);
        assertThat(records.get(OFF_YEAR_SECOND).totalYieldKg()).isEqualTo(SECOND_OFF_KG);
    }

    @Test
    @DisplayName("DELETE harvest record recalculates the Hoblyn BBI over the remaining campaigns")
    void shouldRecalculateBbiOverRemainingCampaignsAfterRemoval() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        var bbiBefore = readBbiMetric(plotId);
        // Over all four campaigns: (8000/16000 + 9000/17000 + 8000/18000) / 3 = 0.491285 -> 0.491
        assertThat(bbiBefore.value()).isEqualTo(0.491);
        assertThat(bbiBefore.evaluatedYearsCount()).isEqualTo(4);
        assertThat(bbiBefore.sampleSufficiency()).isEqualTo("SUFFICIENT");

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED)))
                .andExpect(status().isOk());

        var bbiAfter = readBbiMetric(plotId);
        // Over 12000 (2021), 13000 (2023), 5000 (2024):
        // (1000/25000 + 8000/18000) / 2 = 0.242222 -> 0.242
        assertThat(bbiAfter.value()).isEqualTo(0.242);
        assertThat(bbiAfter.evaluatedYearsCount()).isEqualTo(3);
        assertThat(bbiAfter.sampleSufficiency()).isEqualTo("SUFFICIENT");
        assertThat(bbiAfter.value()).isNotEqualTo(bbiBefore.value());
    }

    @Test
    @DisplayName("DELETE down to two campaigns reports INSUFFICIENT sample sufficiency")
    void shouldReportInsufficientSampleWhenTwoCampaignsRemain() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_SECOND)))
                .andExpect(status().isOk());

        assertThat(countHarvestRows(plotId)).isEqualTo(2);
        assertThat(listHarvestRecords(plotId)).containsOnlyKeys(ON_YEAR_FIRST, ON_YEAR_SECOND);

        var bbi = readBbiMetric(plotId);
        // Only one consecutive pair survives: |13000 - 12000| / 25000 = 0.04
        assertThat(bbi.value()).isEqualTo(0.04);
        assertThat(bbi.evaluatedYearsCount()).isEqualTo(2);
        assertThat(bbi.sampleSufficiency()).isEqualTo("INSUFFICIENT");
    }

    @Test
    @DisplayName("A campaign year removed with DELETE can be recorded again without a 409 conflict")
    void shouldAllowRerecordingTheRemovedCampaignYear() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED)))
                .andExpect(status().isOk());

        recordCampaign(plotId, OFF_YEAR_REMOVED, 9500.0, 6000.0, 3500.0);

        assertThat(countHarvestRows(plotId)).isEqualTo(4);
        var records = listHarvestRecords(plotId);
        assertThat(records).containsOnlyKeys(ON_YEAR_FIRST, OFF_YEAR_REMOVED, ON_YEAR_SECOND, OFF_YEAR_SECOND);
        assertThat(records.get(OFF_YEAR_REMOVED).totalYieldKg()).isEqualTo(9500.0);
    }

    // ---------------------------------------------------------------------------------------------
    // US21 acceptance criterion 1 - Rectify
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("PUT rectification persists the new yield and recalculates the Hoblyn BBI")
    void shouldPersistRectificationAndRecalculateBbi() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        var rectifiedId = recordIdFor(plotId, OFF_YEAR_REMOVED);
        rectifyCampaign(plotId, rectifiedId, 11000.0, 7000.0, 4000.0);

        assertThat(totalYieldKgInDatabase(plotId, OFF_YEAR_REMOVED)).isEqualTo(11000.0);

        var records = listHarvestRecords(plotId);
        assertThat(records.get(OFF_YEAR_REMOVED).id()).isEqualTo(rectifiedId);
        assertThat(records.get(OFF_YEAR_REMOVED).totalYieldKg()).isEqualTo(11000.0);
        assertThat(records.get(OFF_YEAR_REMOVED).bearingClassification()).isEqualTo("BALANCED");

        var bbi = readBbiMetric(plotId);
        // Over 12000 (2021), 11000 (2022), 13000 (2023), 5000 (2024):
        // (1000/23000 + 2000/24000 + 8000/18000) / 3 = 0.190418 -> 0.190
        assertThat(bbi.value()).isEqualTo(0.190);
        assertThat(bbi.evaluatedYearsCount()).isEqualTo(4);
        assertThat(bbi.sampleSufficiency()).isEqualTo("SUFFICIENT");
    }

    // ---------------------------------------------------------------------------------------------
    // Optimistic locking - a stale If-Match must reject and persist nothing
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE with a stale If-Match returns 412 and changes nothing")
    void shouldRejectRemovalWithStaleIfMatchAndChangeNothing() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        var currentRevision = currentTrackerRevision(plotId);
        assertThat(currentRevision).isPositive();

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED))
                        .header("If-Match", quoted(currentRevision - 1)))
                .andExpect(status().isPreconditionFailed());

        assertThat(countHarvestRows(plotId)).isEqualTo(4);
        assertThat(campaignYearsInDatabase(plotId))
                .containsExactlyInAnyOrder(ON_YEAR_FIRST, OFF_YEAR_REMOVED, ON_YEAR_SECOND, OFF_YEAR_SECOND);
        assertThat(currentTrackerRevision(plotId)).isEqualTo(currentRevision);
    }

    @Test
    @DisplayName("PUT with a stale If-Match returns 412 and changes nothing")
    void shouldRejectRectificationWithStaleIfMatchAndChangeNothing() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        var currentRevision = currentTrackerRevision(plotId);
        assertThat(currentRevision).isPositive();

        var payload = """
                {
                    "totalYieldKg": 11000.0,
                    "greenKg": 7000.0,
                    "blackKg": 4000.0
                }
                """;
        mvc.perform(put("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED))
                        .header("If-Match", quoted(currentRevision - 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPreconditionFailed());

        assertThat(countHarvestRows(plotId)).isEqualTo(4);
        assertThat(totalYieldKgInDatabase(plotId, OFF_YEAR_REMOVED)).isEqualTo(REMOVED_OFF_KG);
        assertThat(currentTrackerRevision(plotId)).isEqualTo(currentRevision);
    }

    @Test
    @DisplayName("DELETE with the current If-Match succeeds, proving 412 is not a blanket rejection")
    void shouldAcceptRemovalWithCurrentIfMatch() throws Exception {
        var plotId = createPlot();
        seedAlternatingSeries(plotId);

        var currentRevision = currentTrackerRevision(plotId);

        mvc.perform(delete("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordIdFor(plotId, OFF_YEAR_REMOVED))
                        .header("If-Match", quoted(currentRevision)))
                .andExpect(status().isOk());

        assertThat(countHarvestRows(plotId)).isEqualTo(3);
    }

    // ---------------------------------------------------------------------------------------------
    // Fixtures and helpers
    // ---------------------------------------------------------------------------------------------

    private String createPlot() throws Exception {
        var payload = """
                {
                  "name": "Cuartel US21 %s",
                  "variety": "ARBEQUINA",
                  "polygonGeoJson": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                  "rowSpacingM": 7.0,
                  "treeSpacingM": 5.0
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        var body = mvc.perform(post("/api/v1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private void seedAlternatingSeries(String plotId) throws Exception {
        recordCampaign(plotId, ON_YEAR_FIRST, FIRST_ON_KG, 8000.0, 4000.0);
        recordCampaign(plotId, OFF_YEAR_REMOVED, REMOVED_OFF_KG, 2500.0, 1500.0);
        recordCampaign(plotId, ON_YEAR_SECOND, SECOND_ON_KG, 8500.0, 4500.0);
        recordCampaign(plotId, OFF_YEAR_SECOND, SECOND_OFF_KG, 3000.0, 2000.0);
    }

    private String recordCampaign(String plotId, int campaignYear, double totalYieldKg, double greenKg, double blackKg)
            throws Exception {
        var payload = String.format(Locale.ROOT, """
                {
                  "campaignYear": %d,
                  "totalYieldKg": %.1f,
                  "greenKg": %.1f,
                  "blackKg": %.1f
                }
                """, campaignYear, totalYieldKg, greenKg, blackKg);

        var body = mvc.perform(post("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    private void rectifyCampaign(String plotId, String recordId, double totalYieldKg, double greenKg, double blackKg)
            throws Exception {
        var payload = String.format(Locale.ROOT, """
                {
                  "totalYieldKg": %.1f,
                  "greenKg": %.1f,
                  "blackKg": %.1f,
                  "notes": "Correction after field recalibration"
                }
                """, totalYieldKg, greenKg, blackKg);

        mvc.perform(put("/api/v1/plots/{plotId}/harvest-records/{recordId}", plotId, recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private Map<Integer, HarvestRecordView> listHarvestRecords(String plotId) throws Exception {
        var body = mvc.perform(get("/api/v1/plots/{plotId}/harvest-records", plotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> raw = objectMapper.readValue(body, new TypeReference<>() {
        });
        Map<Integer, HarvestRecordView> byYear = new LinkedHashMap<>();
        for (Map<String, Object> entry : raw) {
            byYear.put(
                    ((Number) entry.get("campaignYear")).intValue(),
                    new HarvestRecordView(
                            (String) entry.get("id"),
                            ((Number) entry.get("totalYieldKg")).doubleValue(),
                            (String) entry.get("bearingClassification")
                    )
            );
        }
        return byYear;
    }

    private BbiView readBbiMetric(String plotId) throws Exception {
        var body = mvc.perform(get("/api/v1/plots/{plotId}/metrics", plotId)
                        .param("metricName", "BIENNIAL_BEARING_INDEX")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var metric = objectMapper.readTree(body).get(0);
        return new BbiView(
                metric.get("value").asDouble(),
                metric.get("details").get("evaluatedYearsCount").asInt(),
                metric.get("details").get("sampleSufficiency").asText()
        );
    }

    private String recordIdFor(String plotId, int campaignYear) throws Exception {
        return listHarvestRecords(plotId).get(campaignYear).id();
    }

    // --- raw database assertions -----------------------------------------------------------------

    private int countHarvestRows(String plotId) {
        return jdbc.queryForObject(
                "select count(*) from harvest_records where plot_id = ?", Integer.class, UUID.fromString(plotId));
    }

    private List<Integer> campaignYearsInDatabase(String plotId) {
        return jdbc.queryForList(
                "select campaign_year from harvest_records where plot_id = ? order by campaign_year",
                Integer.class, UUID.fromString(plotId));
    }

    private double totalYieldKgInDatabase(String plotId, int campaignYear) {
        return jdbc.queryForObject(
                "select total_yield_kg from harvest_records where plot_id = ? and campaign_year = ?",
                Double.class, UUID.fromString(plotId), campaignYear);
    }

    private long currentTrackerRevision(String plotId) {
        return jdbc.queryForObject(
                "select revision from chill_trackers where plot_id = ?", Long.class, UUID.fromString(plotId));
    }

    private static String quoted(long revision) {
        return "\"" + revision + "\"";
    }

    private record HarvestRecordView(String id, double totalYieldKg, String bearingClassification) {
    }

    private record BbiView(double value, int evaluatedYearsCount, String sampleSufficiency) {
    }
}