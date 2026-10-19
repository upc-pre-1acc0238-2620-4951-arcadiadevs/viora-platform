package com.arcadiadevs.viora.platform.profiles.interfaces.rest;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profiles against the whole application: two real accounts, each one only reaching its own profile.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profileownership;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Profile ownership integration")
class ProfileOwnershipIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserAccountCommandService userAccountCommandService;

    private MockMvc mvc;
    private String ana;
    private String bruno;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        // The database is shared by the tests of the class, so every run signs up new addresses
        var run = UUID.randomUUID().toString().substring(0, 8);
        ana = signUp("ana-" + run + "@viora.ag", "Ana Quispe");
        bruno = signUp("bruno-" + run + "@viora.ag", "Bruno Rojas");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A user reads and updates their own profile, created at sign-up, whatever the case of the id")
    void ownProfileIsReachable() throws Exception {
        authenticateAs(ana);

        mvc.perform(get("/api/v1/profiles/" + ana.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Quispe"));
        mvc.perform(put("/api/v1/profiles/" + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Ana Quispe Mamani\", \"country\": \"PE\", \"phoneNumber\": \"+51912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Quispe Mamani"));
    }

    @Test
    @DisplayName("A user can neither read nor change (PUT or PATCH) another user's profile, which is left untouched")
    void anotherUsersProfileIsForbidden() throws Exception {
        authenticateAs(ana);
        var stolen = "{\"fullName\": \"Robado\", \"country\": \"PE\", \"phoneNumber\": \"+51912345678\"}";

        mvc.perform(get("/api/v1/profiles/" + bruno))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        mvc.perform(put("/api/v1/profiles/" + bruno).contentType(MediaType.APPLICATION_JSON).content(stolen))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/profiles/" + bruno).contentType(MediaType.APPLICATION_JSON).content(stolen))
                .andExpect(status().isForbidden());

        authenticateAs(bruno);
        mvc.perform(get("/api/v1/profiles/" + bruno))
                .andExpect(jsonPath("$.fullName").value("Bruno Rojas"));
    }

    @Test
    @DisplayName("Without a session the profile answers 401, and profiles can no longer be created by POST")
    void noSessionAndNoCreation() throws Exception {
        mvc.perform(get("/api/v1/profiles/" + ana))
                .andExpect(status().isUnauthorized());

        authenticateAs(ana);
        mvc.perform(post("/api/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"" + bruno + "\", \"fullName\": \"X\", \"country\": \"PE\", \"phoneNumber\": \"+51912345678\"}"))
                .andExpect(status().isNotFound());
    }

    private String signUp(String email, String fullName) {
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                "ROLE_PRODUCER", true, fullName, "PE", "+51987654321"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }

    private static void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
