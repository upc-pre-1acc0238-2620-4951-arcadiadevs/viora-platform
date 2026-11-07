package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.configuration;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The real security filter chain with real tokens: which part of the API answers each role.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:roleaccess;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Access to the API by role")
class RoleAccessIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserAccountRepository accounts;
    @Autowired TokenService tokenService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    @DisplayName("A technical manager is refused outside profiles and subscriptions, with the usual error body")
    void technicalManagerIsRefusedInProducerApi() throws Exception {
        var token = "Bearer " + tokenOf(accountOf(UserRole.ROLE_TECHNICAL_MANAGER));

        for (var path : new String[]{"/api/v1/plots", "/api/v1/samplings", "/api/v1/assistant/daily-summary"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        }
    }

    @Test
    @DisplayName("A technical manager reaches subscriptions and its profile, and a producer the rest of the API")
    void eachRoleReachesItsApi() throws Exception {
        var managerAccount = accountOf(UserRole.ROLE_TECHNICAL_MANAGER);
        var manager = "Bearer " + tokenOf(managerAccount);
        var producer = "Bearer " + tokenOf(accountOf(UserRole.ROLE_PRODUCER));

        mvc.perform(get("/api/v1/subscriptions").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isNoContent());
        var profile = mvc.perform(get("/api/v1/profiles/" + managerAccount.id().userId())
                .header(HttpHeaders.AUTHORIZATION, manager)).andReturn().getResponse().getStatus();
        assertThat(profile).isNotIn(401, 403);
        mvc.perform(get("/api/v1/plots").header(HttpHeaders.AUTHORIZATION, producer)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Only a technical manager reaches its license, and a producer is refused with the usual error body")
    void licenseIsTheManagers() throws Exception {
        var manager = "Bearer " + tokenOf(accountOf(UserRole.ROLE_TECHNICAL_MANAGER));
        var producer = "Bearer " + tokenOf(accountOf(UserRole.ROLE_PRODUCER));

        mvc.perform(get("/api/v1/cooperative-licenses").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/cooperative-licenses").header(HttpHeaders.AUTHORIZATION, producer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(get("/api/v1/cooperative-licenses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/invitation-codes").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/invitation-codes").header(HttpHeaders.AUTHORIZATION, producer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("Both roles read sectors; only a manager edits the list and only a producer sectors a plot")
    void sectorsAreSharedByRole() throws Exception {
        var manager = "Bearer " + tokenOf(accountOf(UserRole.ROLE_TECHNICAL_MANAGER));
        var producer = "Bearer " + tokenOf(accountOf(UserRole.ROLE_PRODUCER));
        var plot = "/api/v1/plot-sector-assignments/" + UUID.randomUUID();

        // Past the filter, the territory services answer for the missing license with their own reason.
        for (var token : new String[]{manager, producer}) {
            mvc.perform(get("/api/v1/sector-zones").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.reason").exists());
            mvc.perform(get("/api/v1/plot-sector-assignments").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(jsonPath("$.reason").exists());
        }
        mvc.perform(post("/api/v1/sector-zones").header(HttpHeaders.AUTHORIZATION, producer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Sama\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(post(plot + "/resolutions").header(HttpHeaders.AUTHORIZATION, producer))
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(put(plot).header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Sama\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(post("/api/v1/sector-zones").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Sama\"}"))
                .andExpect(jsonPath("$.reason").value("territory.manager.no_license"));
        mvc.perform(get("/api/v1/sector-zones")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Only a technical manager reads members, member plots, sector alerts and intake")
    void memberViewIsTheManagers() throws Exception {
        var manager = "Bearer " + tokenOf(accountOf(UserRole.ROLE_TECHNICAL_MANAGER));
        var producer = "Bearer " + tokenOf(accountOf(UserRole.ROLE_PRODUCER));

        for (var path : new String[]{"/api/v1/cooperative-members", "/api/v1/member-plots", "/api/v1/sector-alerts",
                "/api/v1/intake-forecasts"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, producer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
            // Past the filter, a manager without a license gets the territory reason.
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, manager))
                    .andExpect(jsonPath("$.reason").value("territory.manager.no_license"));
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Without a token the producer API answers 401 and the plan catalog stays public")
    void withoutToken() throws Exception {
        mvc.perform(get("/api/v1/plots")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/subscription-plans")).andExpect(status().isOk());
    }

    private UserAccountSnapshot accountOf(UserRole role) {
        return accounts.save(UserAccountFixtures.activeAccount(
                UUID.randomUUID() + "@viora.ag", "hashed:Password123", role)).snapshot();
    }

    private String tokenOf(UserAccountSnapshot account) {
        return tokenService.generateAccessToken(account.id().userId(), account.email().value(),
                account.role().name(), account.credentialsVersion());
    }
}
