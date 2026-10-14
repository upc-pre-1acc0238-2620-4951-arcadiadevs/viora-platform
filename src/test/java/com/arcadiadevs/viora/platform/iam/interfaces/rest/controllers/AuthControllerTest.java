package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController REST Endpoint Unit/MockMvc Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserAccountCommandService commandService;

    @Mock
    private UserAccountQueryService queryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BearerTokenService tokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        var controller = new AuthController(
                commandService,
                queryService,
                passwordEncoder,
                tokenService,
                eventPublisher
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should create user and return 201 with session resource")
    void shouldSignUpSuccessfully() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(passwordEncoder.encode("Password123!")).thenReturn("$argon2id$encoded");
        when(commandService.handle(any(RegisterUserAccountCommand.class))).thenReturn(Result.success(userId));
        when(tokenService.generateAccessToken(eq(userId), eq("productor@viora.ag"), eq("ROLE_PRODUCER")))
                .thenReturn("mock.jwt.token");
        when(tokenService.getAccessTokenValiditySeconds()).thenReturn(900L);

        String json = """
                {
                  "email": "productor@viora.ag",
                  "password": "Password123!",
                  "role": "ROLE_PRODUCER",
                  "profile": {
                    "fullName": "Carlos Mendoza",
                    "country": "PE",
                    "phoneNumber": "+51987654321"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.role").value("ROLE_PRODUCER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in should return 200 with tokens on valid credentials")
    void shouldSignInSuccessfully() throws Exception {
        var account = UserAccount.register(
                new EmailAddress("productor@viora.ag"),
                new PasswordHash("$argon2id$encoded"),
                UserRole.ROLE_PRODUCER
        );

        when(queryService.handle(any(com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery.class)))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123!", "$argon2id$encoded")).thenReturn(true);
        when(tokenService.generateAccessToken(any(), eq("productor@viora.ag"), eq("ROLE_PRODUCER")))
                .thenReturn("mock.jwt.token");
        when(tokenService.getAccessTokenValiditySeconds()).thenReturn(900L);

        String json = """
                {
                  "email": "productor@viora.ag",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/password-reset-tokens should return 202 Accepted")
    void shouldRequestPasswordReset() throws Exception {
        String json = "{\"email\": \"productor@viora.ag\"}";

        mockMvc.perform(post("/api/v1/auth/password-reset-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("PUT /api/v1/auth/password-reset-tokens/{token} should return 204 No Content")
    void shouldResetPassword() throws Exception {
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("$argon2id$newencoded");
        when(commandService.handle(any(ResetUserPasswordCommand.class))).thenReturn(Result.success(null));

        String json = "{\"newPassword\": \"NewPassword123!\"}";

        mockMvc.perform(put("/api/v1/auth/password-reset-tokens/valid-token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }
}
