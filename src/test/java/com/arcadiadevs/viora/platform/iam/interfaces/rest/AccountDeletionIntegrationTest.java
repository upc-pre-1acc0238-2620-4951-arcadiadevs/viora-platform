package com.arcadiadevs.viora.platform.iam.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.subscription.infrastructure.seeding.SubscriptionTestCodesSeeder;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Account deletion (P100) against the whole application: a producer with a plot, a logbook note, a plan, a profile,
 * a session and an assistant answer deletes the account, and nothing of it is left.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:accountdeletion;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.subscription.seed.enabled=true",
        "viora.demo-account.email=demo@viora.test"
})
@DisplayName("Account deletion")
class AccountDeletionIntegrationTest {

    private static final String URL = "/api/v1/auth/account-deletions";
    private static final String PASSWORD = "Password123";

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired PasswordHasher passwordHasher;
    @Autowired ProfileCommandService profiles;
    @Autowired UserSessionCommandService sessions;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deletes the account and everything it owns; a wrong password deletes nothing first")
    void deletesEverything() throws Exception {
        var producer = activeProducer("producer-" + UUID.randomUUID().toString().substring(0, 8) + "@viora.ag");
        profiles.handle(new CreateProfileCommand(producer, "Teodoro Mamani", "PE", "987654321"));
        sessions.handle(new OpenUserSessionCommand(producer));
        authenticateAs(producer);
        var plotId = createPlot();
        mvc.perform(post("/api/v1/plots/" + plotId + "/field-notes").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"%s","type":"IRRIGATION","text":"Riego.","observedOn":"%s"}
                        """.formatted(UUID.randomUUID(), LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/cooperative-code-redemptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + SubscriptionTestCodesSeeder.CODES.firstEntry().getKey() + "\"}"))
                .andExpect(status().isCreated());
        // Two harvests enable an assistant question, so the producer keeps one answer
        for (var harvest : List.of(List.of(2024, 1000), List.of(2025, 3000))) {
            mvc.perform(post("/api/v1/plots/" + plotId + "/harvest-records").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"campaignYear\":%d,\"totalYieldKg\":%d}".formatted(harvest.get(0),
                                    harvest.get(1))))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/assistant/answers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionId":"alternation.vs_campaigns","plotId":"%s","clientRequestId":"%s"}
                                """.formatted(plotId, UUID.randomUUID())))
                .andExpect(status().isCreated());

        delete("Wrong-password-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.reason").value("user.password.current_invalid"));
        assertThat(owned(producer)).containsOnly(1);

        delete(PASSWORD).andExpect(status().isNoContent());

        assertThat(owned(producer)).containsOnly(0);
    }

    @Test
    @DisplayName("The shared demo account cannot be deleted")
    void demoAccountIsProtected() throws Exception {
        var demo = activeProducer("demo@viora.test");
        authenticateAs(demo);

        delete(PASSWORD).andExpect(status().isForbidden());

        assertThat(count("select count(*) from user_accounts where id = ?", UUID.fromString(demo))).isEqualTo(1);
    }

    @Test
    @DisplayName("Without a session nothing is deleted")
    void anonymousRequestIsRejected() throws Exception {
        delete(PASSWORD).andExpect(status().isUnauthorized());
    }

    private ResultActions delete(String password) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"" + password + "\"}"));
    }

    /** Rows the producer still has in each table touched by the deletion. */
    private List<Integer> owned(String producer) {
        var id = UUID.fromString(producer);
        return List.of(
                count("select count(*) from user_accounts where id = ?", id),
                count("select count(*) from user_sessions where user_id = ?", id),
                count("select count(*) from profiles where user_id = ?", id),
                count("select count(*) from plots where producer_id = ?", producer),
                count("select count(*) from field_notes where producer_id = ?", producer),
                count("select count(*) from subscriptions where producer_id = ?", id),
                count("select count(*) from assistant_answers where producer_id = ?", producer));
    }

    private int count(String sql, Object arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }

    private String activeProducer(String email) {
        var hash = passwordHasher.hash(new NewPassword(PASSWORD)).value();
        var account = accounts.save(UserAccountFixtures.activeAccount(email, hash, UserRole.ROLE_PRODUCER));
        return account.snapshot().id().userId();
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
