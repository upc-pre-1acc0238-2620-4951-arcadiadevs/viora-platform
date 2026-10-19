package com.arcadiadevs.viora.platform.profiles.interfaces.rest.controllers;

import org.mockito.ArgumentCaptor;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController REST Endpoint Unit/MockMvc Tests")
class ProfileControllerTest {

    private static final String UPDATE_JSON = """
            {
              "fullName": "Carlos Mendoza Alarcón",
              "country": "ES",
              "phoneNumber": "+34612345678"
            }
            """;

    private final String userId = UUID.randomUUID().toString();

    private MockMvc mockMvc;

    @Mock
    private ProfileCommandService commandService;

    @Mock
    private ProfileQueryService queryService;

    @BeforeEach
    void setUp() {
        // Standalone MockMvc does not apply method security; ProfileOwnershipIntegrationTest covers who may call
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(commandService, queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{userId} should return the authenticated user's own profile")
    void shouldGetProfileSuccessfully() throws Exception {
        when(queryService.handle(any(GetProfileByUserIdQuery.class))).thenReturn(Result.success(profileOf(userId)));

        mockMvc.perform(get("/api/v1/profiles/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza"));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{userId} should answer 404 when the account has no profile")
    void shouldAnswerNotFoundWithoutProfile() throws Exception {
        when(queryService.handle(any(GetProfileByUserIdQuery.class)))
                .thenReturn(Result.failure(ApplicationError.notFound("profile", userId)));

        mockMvc.perform(get("/api/v1/profiles/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/v1/profiles/{userId} should update the authenticated user's own profile")
    void shouldUpdateProfileSuccessfully() throws Exception {
        var profile = Profile.create(new UserId(userId), new FullName("Carlos Mendoza Alarcón"),
                new CountryCode("ES"), new PhoneNumber("+34612345678"));
        when(commandService.handle(any(UpdateContactProfileCommand.class))).thenReturn(Result.success(profile));

        mockMvc.perform(put("/api/v1/profiles/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza Alarcón"))
                .andExpect(jsonPath("$.country").value("ES"));
    }

    @Test
    @DisplayName("PATCH /api/v1/profiles/{userId} should send the same update command as PUT")
    void shouldPatchLikePut() throws Exception {
        when(commandService.handle(any(UpdateContactProfileCommand.class))).thenReturn(Result.success(profileOf(userId)));

        mockMvc.perform(patch("/api/v1/profiles/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(UpdateContactProfileCommand.class);
        verify(commandService).handle(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UpdateContactProfileCommand(userId, "Carlos Mendoza Alarcón",
                "ES", "+34612345678"));
    }

    @Test
    @DisplayName("POST should not create profiles (they are created at sign-up) and answer 405 with the allowed methods")
    void shouldNotExposeProfileCreation() throws Exception {
        mockMvc.perform(post("/api/v1/profiles/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET, PATCH, PUT"))
                .andExpect(jsonPath("$.code").value("ROUTE_METHOD_NOT_ALLOWED"));
        verifyNoInteractions(commandService);
    }

    private static Profile profileOf(String userId) {
        return Profile.create(new UserId(userId), new FullName("Carlos Mendoza"),
                new CountryCode("PE"), new PhoneNumber("+51987654321"));
    }
}
