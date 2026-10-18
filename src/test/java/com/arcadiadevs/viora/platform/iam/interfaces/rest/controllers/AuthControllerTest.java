package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @BeforeEach
    void setUp() {
        var controller = new AuthController(
                commandService,
                queryService,
                passwordEncoder,
                tokenService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should accept a pending account with 202 and no session")
    void shouldSignUpSuccessfully() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(passwordEncoder.encode("Password123!")).thenReturn("$argon2id$encoded");
        when(commandService.handle(any(RegisterUserAccountCommand.class))).thenReturn(Result.success(userId));

        String json = """
                {
                  "email": "productor@viora.ag",
                  "password": "Password123!",
                  "role": "ROLE_PRODUCER",
                  "termsAccepted": true,
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
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should reject a request without termsAccepted")
    void shouldRejectSignUpWithoutTerms() throws Exception {
        String json = """
                {"email": "productor@viora.ag", "password": "Password123!", "role": "ROLE_PRODUCER"}
                """;

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in should answer 403 EMAIL_VERIFICATION_FORBIDDEN for a pending account")
    void shouldRejectSignInOfPendingAccount() throws Exception {
        var account = UserAccountFixtures.pendingAccount("productor@viora.ag", "$argon2id$encoded");
        when(queryService.handle(any(com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery.class)))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123!", "$argon2id$encoded")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"password\": \"Password123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verifications should return the session once the email is verified")
    void shouldReturnSessionOnEmailVerification() throws Exception {
        var account = UserAccountFixtures.activeAccount("productor@viora.ag", "$argon2id$encoded", UserRole.ROLE_PRODUCER);
        when(commandService.handle(any(VerifyEmailCommand.class)))
                .thenReturn(Result.success(account.snapshot().id().userId()));
        when(queryService.handle(any(com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery.class)))
                .thenReturn(Optional.of(account));
        when(tokenService.generateAccessToken(any(), eq("productor@viora.ag"), eq("ROLE_PRODUCER")))
                .thenReturn("mock.jwt.token");

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"code\": \"482913\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.userId").value(account.snapshot().id().userId()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verifications should report the remaining attempts on a wrong code")
    void shouldReportRemainingAttemptsOnWrongCode() throws Exception {
        when(commandService.handle(any(VerifyEmailCommand.class))).thenReturn(Result.failure(
                ApplicationError.businessRuleViolation("email_verification", "user.email_verification.invalid_code",
                        java.util.Map.of("reason", "user.email_verification.invalid_code", "remainingAttempts", 2))));

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"code\": \"000000\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.reason").value("user.email_verification.invalid_code"))
                .andExpect(jsonPath("$.remainingAttempts").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verification-codes should answer 429 with Retry-After within the cooldown")
    void shouldAnswerTooManyRequestsWithinCooldown() throws Exception {
        when(commandService.handle(any(ResendEmailVerificationCommand.class))).thenReturn(Result.failure(
                ApplicationError.tooManyRequests("email-verification", "user.email_verification.resend_too_soon", 30L)));

        mockMvc.perform(post("/api/v1/auth/email-verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verification-codes should answer 202 when the code is sent")
    void shouldAcceptResend() throws Exception {
        when(commandService.handle(any(ResendEmailVerificationCommand.class))).thenReturn(Result.success(null));

        mockMvc.perform(post("/api/v1/auth/email-verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in should return 200 with tokens on valid credentials")
    void shouldSignInSuccessfully() throws Exception {
        var account = UserAccountFixtures.activeAccount("productor@viora.ag", "$argon2id$encoded", UserRole.ROLE_PRODUCER);

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
    @DisplayName("PUT /api/v1/auth/password-reset-tokens/{token} with email should return 204 No Content")
    void shouldResetPassword() throws Exception {
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("$argon2id$newencoded");
        when(commandService.handle(any(ResetUserPasswordCommand.class))).thenReturn(Result.success(null));

        String json = "{\"email\": \"productor@viora.ag\", \"newPassword\": \"NewPassword123!\"}";

        mockMvc.perform(put("/api/v1/auth/password-reset-tokens/123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }
}
