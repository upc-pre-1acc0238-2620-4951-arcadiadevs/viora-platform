package com.arcadiadevs.viora.platform.profiles.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController REST Endpoint Unit/MockMvc Tests")
class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProfileCommandService commandService;

    @Mock
    private ProfileQueryService queryService;

    @BeforeEach
    void setUp() {
        var controller = new ProfileController(commandService, queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/profiles should return 201 Created")
    void shouldCreateProfileSuccessfully() throws Exception {
        String userId = UUID.randomUUID().toString();
        var profile = Profile.create(
                new UserId(userId),
                new FullName("Carlos Mendoza"),
                new CountryCode("PE"),
                new PhoneNumber("+51987654321")
        );

        when(commandService.handle(any(CreateProfileCommand.class)))
                .thenReturn(Result.success(profile.snapshot().id().profileId()));
        when(queryService.handle(any(com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery.class)))
                .thenReturn(Optional.of(profile));

        String json = """
                {
                  "userId": "%s",
                  "fullName": "Carlos Mendoza",
                  "country": "PE",
                  "phoneNumber": "+51987654321"
                }
                """.formatted(userId);

        mockMvc.perform(post("/api/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.country").value("PE"))
                .andExpect(jsonPath("$.phoneNumber").value("+51987654321"));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{userId} should return 200 OK with profile")
    void shouldGetProfileSuccessfully() throws Exception {
        String userId = UUID.randomUUID().toString();
        var profile = Profile.create(
                new UserId(userId),
                new FullName("Carlos Mendoza"),
                new CountryCode("PE"),
                new PhoneNumber("+51987654321")
        );

        when(queryService.handle(any(com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery.class)))
                .thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/profiles/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza"));
    }

    @Test
    @DisplayName("PUT /api/v1/profiles/{userId} should return 200 OK with updated profile")
    void shouldUpdateProfileSuccessfully() throws Exception {
        String userId = UUID.randomUUID().toString();
        var profile = Profile.create(
                new UserId(userId),
                new FullName("Carlos Mendoza Alarcón"),
                new CountryCode("ES"),
                new PhoneNumber("+34612345678")
        );

        when(commandService.handle(any(UpdateContactProfileCommand.class)))
                .thenReturn(Result.success(profile));

        String json = """
                {
                  "fullName": "Carlos Mendoza Alarcón",
                  "country": "ES",
                  "phoneNumber": "+34612345678"
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza Alarcón"))
                .andExpect(jsonPath("$.country").value("ES"));
    }
}
