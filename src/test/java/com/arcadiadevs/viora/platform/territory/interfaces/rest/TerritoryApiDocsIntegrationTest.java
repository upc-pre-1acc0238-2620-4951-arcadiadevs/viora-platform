package com.arcadiadevs.viora.platform.territory.interfaces.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The published API description of the territory endpoints: one name per aggregate.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:territoryapidocs;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Territory API description")
class TerritoryApiDocsIntegrationTest {

    @Autowired WebApplicationContext context;

    @Test
    @DisplayName("keeps the sector, member and plot routes under their own names")
    void describesTheTerritoryApi() throws Exception {
        var body = MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        var docs = JsonPath.parse(body);

        for (var route : List.of(
                "$.paths['/api/v1/sector-zones'].get",
                "$.paths['/api/v1/sector-zones'].post",
                "$.paths['/api/v1/sector-zones/{id}'].get",
                "$.paths['/api/v1/sector-zones/{id}'].patch")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Sector Zones"));
        }
        for (var route : List.of(
                "$.paths['/api/v1/plot-sector-assignments'].get",
                "$.paths['/api/v1/plot-sector-assignments/{plotId}'].get",
                "$.paths['/api/v1/plot-sector-assignments/{plotId}'].put",
                "$.paths['/api/v1/plot-sector-assignments/{plotId}/resolutions'].post")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Plot Sector Assignments"));
        }
        for (var route : List.of(
                "$.paths['/api/v1/cooperative-members'].get",
                "$.paths['/api/v1/cooperative-members/{id}'].get")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Cooperative Members"));
        }
        for (var route : List.of(
                "$.paths['/api/v1/member-plots'].get",
                "$.paths['/api/v1/member-plots/{id}'].get",
                "$.paths['/api/v1/member-plots/{id}/provisional-dossier'].get")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Member Plots"));
        }
        Map<String, Object> alerts = docs.read("$.paths['/api/v1/sector-alerts'].get");
        assertThat(alerts.get("tags")).isEqualTo(List.of("Sector Alerts"));
        for (var route : List.of(
                "$.paths['/api/v1/intake-forecasts'].get",
                "$.paths['/api/v1/intake-forecasts/{campaignYear}'].get")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Intake Forecasts"));
        }
        Map<String, Object> release = docs.read("$.paths['/api/v1/cooperative-licenses/{id}/member-releases'].post");
        assertThat(release.get("tags")).isEqualTo(List.of("Cooperative Licenses"));
    }
}
