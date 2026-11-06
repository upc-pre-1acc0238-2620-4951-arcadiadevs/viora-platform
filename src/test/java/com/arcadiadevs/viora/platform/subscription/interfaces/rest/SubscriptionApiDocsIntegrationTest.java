package com.arcadiadevs.viora.platform.subscription.interfaces.rest;

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
 * The published API description: the subscription endpoints live together under one name, and no group falls back to
 * the generated name of its controller.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:subscriptionapidocs;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Subscription API description")
class SubscriptionApiDocsIntegrationTest {

    @Autowired WebApplicationContext context;

    @Test
    @DisplayName("keeps the subscription routes under one tag and no tag ends in -controller")
    void describesTheSubscriptionApi() throws Exception {
        var body = MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        var docs = JsonPath.parse(body);

        for (var route : List.of(
                "$.paths['/api/v1/subscription-plans'].get",
                "$.paths['/api/v1/subscriptions'].get",
                "$.paths['/api/v1/subscriptions'].post",
                "$.paths['/api/v1/subscriptions/{id}'].get",
                "$.paths['/api/v1/subscriptions/{id}/checkouts'].post")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Subscriptions"));
        }
        Map<String, Object> redemption = docs.read("$.paths['/api/v1/manager-code-redemptions'].post");
        assertThat(redemption.get("tags")).isEqualTo(List.of("Manager Code Redemptions"));
        Map<String, Object> license = docs.read("$.paths['/api/v1/cooperative-licenses'].get");
        assertThat(license.get("tags")).isEqualTo(List.of("Cooperative Licenses"));
        for (var route : List.of(
                "$.paths['/api/v1/invitation-codes'].post",
                "$.paths['/api/v1/invitation-codes'].get",
                "$.paths['/api/v1/invitation-codes/{id}/revocations'].post")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Invitation Codes"));
        }
        Map<String, Object> preview = docs.read("$.paths['/api/v1/cooperative-code-previews'].post");
        assertThat(preview.get("tags")).isEqualTo(List.of("Cooperative Code Previews"));
        for (var route : List.of(
                "$.paths['/api/v1/data-sharing-consents'].get",
                "$.paths['/api/v1/data-sharing-consents/{id}'].get",
                "$.paths['/api/v1/data-sharing-consents/{id}/withdrawals'].post")) {
            Map<String, Object> operation = docs.read(route);
            assertThat(operation.get("tags")).as(route).isEqualTo(List.of("Data Sharing Consents"));
        }
        List<String> tags = docs.read("$.paths.*.*.tags[*]");
        assertThat(tags).isNotEmpty().noneMatch(tag -> tag.endsWith("-controller"));
    }
}
