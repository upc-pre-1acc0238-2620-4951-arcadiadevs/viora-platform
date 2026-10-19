package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckPasswordResetTokenQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.AuthenticateUserCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.infrastructure.security.SpringSecurityCurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController REST Endpoint Unit/MockMvc Tests")
class AuthControllerTest {

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final AuthenticatedSession SESSION = new AuthenticatedSession(
            "mock.jwt.token", "mock-refresh-token", 900L, USER_ID, "productor@viora.ag", "ROLE_PRODUCER");

    private MockMvc mockMvc;

    @Mock
    private UserAccountCommandService commandService;

    @Mock
    private UserSessionCommandService sessionService;

    @Mock
    private UserAccountQueryService queryService;

    @BeforeEach
    void setUp() {
        var controller = new AuthController(commandService, sessionService, queryService,
                new SpringSecurityCurrentUserProvider(false, "550e8400-e29b-41d4-a716-446655440000"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should accept a pending account with 202 and no session")
    void shouldSignUpSuccessfully() throws Exception {
        when(commandService.handle(any(RegisterUserAccountCommand.class))).thenReturn(Result.success(USER_ID));

        String json = """
                {
                  "email": "productor@viora.ag",
                  "password": "Password123",
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
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        var captor = ArgumentCaptor.forClass(RegisterUserAccountCommand.class);
        verify(commandService).handle(captor.capture());
        // The plain password reaches the application layer, which checks the rules and hashes it
        assertThat(captor.getValue().password()).isEqualTo("Password123");
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should reject a request without termsAccepted")
    void shouldRejectSignUpWithoutTerms() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"password\": \"Password123\", \"role\": \"ROLE_PRODUCER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up should list every broken password rule")
    void shouldListBrokenPasswordRules() throws Exception {
        when(commandService.handle(any(RegisterUserAccountCommand.class))).thenReturn(Result.failure(
                ApplicationError.validationError("password", "user.password.weak",
                        Map.of("violations", List.of("user.password.min_length", "user.password.letters_and_digits")))));

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"password\": \"abc\", \"role\": \"ROLE_PRODUCER\", \"termsAccepted\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0]").value("user.password.min_length"))
                .andExpect(jsonPath("$.violations[1]").value("user.password.letters_and_digits"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in should return 200 with the session on valid credentials")
    void shouldSignInSuccessfully() throws Exception {
        when(sessionService.handle(any(AuthenticateUserCommand.class))).thenReturn(Result.success(SESSION));

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"password\": \"Password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in should answer 403 EMAIL_VERIFICATION_FORBIDDEN for a pending account")
    void shouldRejectSignInOfPendingAccount() throws Exception {
        when(sessionService.handle(any(AuthenticateUserCommand.class))).thenReturn(Result.failure(
                ApplicationError.forbidden("email-verification", "user.email.not_verified")));

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"password\": \"Password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verifications should return the session once the email is verified")
    void shouldReturnSessionOnEmailVerification() throws Exception {
        when(commandService.handle(any(VerifyEmailCommand.class))).thenReturn(Result.success(USER_ID));
        when(sessionService.handle(any(OpenUserSessionCommand.class))).thenReturn(Result.success(SESSION));

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"code\": \"482913\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    @DisplayName("POST /api/v1/auth/email-verifications should report the remaining attempts on a wrong code")
    void shouldReportRemainingAttemptsOnWrongCode() throws Exception {
        when(commandService.handle(any(VerifyEmailCommand.class))).thenReturn(Result.failure(
                ApplicationError.businessRuleViolation("email_verification", "user.email_verification.invalid_code",
                        Map.of("reason", "user.email_verification.invalid_code", "remainingAttempts", 2))));

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\", \"code\": \"000000\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.reason").value("user.email_verification.invalid_code"))
                .andExpect(jsonPath("$.remainingAttempts").value(2));
        verify(sessionService, never()).handle(any(OpenUserSessionCommand.class));
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
    @DisplayName("POST /api/v1/auth/refresh-tokens should return the rotated session")
    void shouldRefreshSession() throws Exception {
        when(sessionService.handle(any(RefreshUserSessionCommand.class))).thenReturn(Result.success(SESSION));

        mockMvc.perform(post("/api/v1/auth/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/passwords should change the password of the authenticated user")
    void shouldChangePasswordOfAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
        when(commandService.handle(any(ChangeUserPasswordCommand.class))).thenReturn(Result.success(USER_ID));

        mockMvc.perform(patch("/api/v1/auth/passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Password123\", \"newPassword\": \"NuevaClave456\"}"))
                .andExpect(status().isNoContent());

        var captor = ArgumentCaptor.forClass(ChangeUserPasswordCommand.class);
        verify(commandService).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().currentRawPassword()).isEqualTo("Password123");
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/passwords should answer 401 without an authenticated user")
    void shouldRejectPasswordChangeWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Password123\", \"newPassword\": \"NuevaClave456\"}"))
                .andExpect(status().isUnauthorized());
        verify(commandService, never()).handle(any(ChangeUserPasswordCommand.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/password-reset-tokens should return 202 Accepted")
    void shouldRequestPasswordReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"productor@viora.ag\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("GET /api/v1/auth/password-reset-tokens/{token} should answer 204 while the link can be used")
    void shouldAcceptUsableResetLink() throws Exception {
        when(queryService.handle(any(CheckPasswordResetTokenQuery.class))).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/password-reset-tokens/Ab9_x-Z"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/auth/password-reset-tokens/{token} should answer 404 for an expired or used link")
    void shouldRejectExpiredResetLink() throws Exception {
        when(queryService.handle(any(CheckPasswordResetTokenQuery.class))).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/password-reset-tokens/Ab9_x-Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/v1/auth/password-reset-tokens/{token} should reset with the link and no email")
    void shouldResetPassword() throws Exception {
        when(commandService.handle(any(ResetUserPasswordCommand.class))).thenReturn(Result.success(null));

        mockMvc.perform(put("/api/v1/auth/password-reset-tokens/Ab9_x-Z")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"NuevaClave456\"}"))
                .andExpect(status().isNoContent());

        var captor = ArgumentCaptor.forClass(ResetUserPasswordCommand.class);
        verify(commandService).handle(captor.capture());
        assertThat(captor.getValue().resetToken()).isEqualTo("Ab9_x-Z");
    }
}
