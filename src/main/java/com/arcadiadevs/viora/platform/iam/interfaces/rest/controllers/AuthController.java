package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.AuthenticateUserCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.SignOutUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckPasswordResetTokenQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.*;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.transform.AuthenticatedSessionResourceFromSessionAssembler;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.transform.RegisterUserAccountCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication, registration, session renewal, and password recovery endpoints.
 */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Identity & Access Management", description = "Endpoints for authentication, credentials, and session lifecycle")
public class AuthController {

    private final UserAccountCommandService userAccountCommandService;
    private final UserSessionCommandService userSessionCommandService;
    private final UserAccountQueryService userAccountQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param userAccountCommandService account use cases: sign-up, email verification and passwords
     * @param userSessionCommandService session use cases: sign-in, opening and renewing sessions
     * @param userAccountQueryService   account queries, such as whether a reset link can still be used
     * @param currentUserProvider       resolves the authenticated user of each request
     */
    public AuthController(
            UserAccountCommandService userAccountCommandService,
            UserSessionCommandService userSessionCommandService,
            UserAccountQueryService userAccountQueryService,
            CurrentUserProvider currentUserProvider
    ) {
        if (userAccountCommandService == null) {
            throw new IllegalArgumentException("user.command_service.null");
        }
        if (userSessionCommandService == null) {
            throw new IllegalArgumentException("user.session_command_service.null");
        }
        if (userAccountQueryService == null) {
            throw new IllegalArgumentException("user.query_service.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("security.current_user_provider.null");
        }
        this.userAccountCommandService = userAccountCommandService;
        this.userSessionCommandService = userSessionCommandService;
        this.userAccountQueryService = userAccountQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up a user account",
            description = "Creates an account pending email verification, with its onboarding profile, and emails a 6-digit verification code. No session is issued until the email is verified. Signing up again with the email of a pending account reuses it with a new code. A weak password answers 400 with every broken rule in violations.",
            security = {})
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        var command = RegisterUserAccountCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<String, ApplicationError> success -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new SignUpAcceptedResource(success.value(), resource.email(),
                            UserAccountStatus.PENDING_VERIFICATION.name()));
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/email-verifications")
    @Operation(summary = "Verify the account email",
            description = "Checks the 6-digit code sent at sign-up, activates the account and returns the session. A wrong code answers 422 with the remaining attempts; after 3 wrong codes a new one must be requested.",
            security = {})
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailResource resource) {
        var result = userAccountCommandService.handle(new VerifyEmailCommand(resource.email(), resource.code()));

        return switch (result) {
            case Result.Success<String, ApplicationError> success ->
                    toSessionResponse(userSessionCommandService.handle(new OpenUserSessionCommand(success.value())));
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/email-verification-codes")
    @Operation(summary = "Resend the email verification code",
            description = "Emails a new 6-digit code to a pending account. Answers 202 for unknown or already verified emails too, and 429 with Retry-After when asked again within 45 seconds.",
            security = {})
    public ResponseEntity<?> resendEmailVerificationCode(@Valid @RequestBody EmailVerificationCodeResource resource) {
        var result = userAccountCommandService.handle(new ResendEmailVerificationCommand(resource.email()));

        return switch (result) {
            case Result.Success<Void, ApplicationError> ignored -> ResponseEntity.accepted().build();
            case Result.Failure<Void, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Authenticate user credentials",
            description = "Validates credentials and returns the access token and a refresh token. A wrong email and a wrong password get the same answer; an account whose email is not verified yet answers 403 with code EMAIL_VERIFICATION_FORBIDDEN.",
            security = {})
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        return toSessionResponse(userSessionCommandService.handle(
                new AuthenticateUserCommand(resource.email(), resource.password())));
    }

    @PostMapping("/refresh-tokens")
    @Operation(summary = "Renew session tokens",
            description = "Rotates the refresh token: the one sent stops working and a new pair is returned. A refresh token used twice closes every session of the account.",
            security = {})
    public ResponseEntity<?> refreshTokens(@Valid @RequestBody RefreshTokenResource resource) {
        return toSessionResponse(userSessionCommandService.handle(
                new RefreshUserSessionCommand(resource.refreshToken())));
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Sign out",
            description = "Closes the session held by the refresh token, so it can no longer be renewed. Always answers 204, even for an unknown or already closed session. The app must also discard its tokens.",
            security = {})
    public ResponseEntity<Void> signOut(@Valid @RequestBody RefreshTokenResource resource) {
        userSessionCommandService.handle(new SignOutUserSessionCommand(resource.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/passwords")
    @Operation(summary = "Change account password",
            description = "Updates the password of the authenticated user after checking the current one. The new password follows the password rules and cannot repeat the last 5. Every other session is closed and this device receives a new session, so it stays signed in.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordResource resource) {
        var userId = currentUserProvider.requireCurrentUserId();
        var command = new ChangeUserPasswordCommand(userId, resource.currentPassword(), resource.newPassword());
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            // The change revoked every session, this one included; a new one keeps this device signed in
            case Result.Success<String, ApplicationError> ignored ->
                    toSessionResponse(userSessionCommandService.handle(new OpenUserSessionCommand(userId)));
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/password-reset-tokens")
    @Operation(summary = "Request password reset",
            description = "Emails a single-use link, valid for 30 minutes, that opens the app to set a new password. Always answers 202, so it does not reveal which emails are registered. A new link replaces the previous one.",
            security = {})
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetResource resource) {
        userAccountCommandService.handle(new RequestPasswordResetCommand(resource.email()));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/password-reset-tokens/{token}")
    @Operation(summary = "Check a password reset link",
            description = "Answers 204 while the link can still be used (show T08) and 404 PASSWORD_RESET_TOKEN_NOT_FOUND when it expired, was already used or replaced (show T09). It does not consume the link.",
            security = {})
    public ResponseEntity<?> checkPasswordResetToken(@PathVariable String token) {
        if (userAccountQueryService.handle(new CheckPasswordResetTokenQuery(token))) {
            return ResponseEntity.noContent().build();
        }
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.notFound("password_reset_token", "user.password_reset.link_expired"));
    }

    @PutMapping("/password-reset-tokens/{token}")
    @Operation(summary = "Reset password with a reset link",
            description = "Consumes the single-use link and sets the new password. The new password follows the password rules and cannot repeat the last 5; every session is closed. An expired, used or replaced link answers 404 PASSWORD_RESET_TOKEN_NOT_FOUND.",
            security = {})
    public ResponseEntity<?> resetPassword(
            @PathVariable String token,
            @Valid @RequestBody ResetPasswordResource resource
    ) {
        var result = userAccountCommandService.handle(new ResetUserPasswordCommand(token, resource.newPassword()));

        return switch (result) {
            case Result.Success<Void, ApplicationError> ignored -> ResponseEntity.noContent().build();
            case Result.Failure<Void, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    private static ResponseEntity<?> toSessionResponse(Result<AuthenticatedSession, ApplicationError> result) {
        return switch (result) {
            case Result.Success<AuthenticatedSession, ApplicationError> success -> ResponseEntity.ok(
                    AuthenticatedSessionResourceFromSessionAssembler.toResourceFromSession(success.value()));
            case Result.Failure<AuthenticatedSession, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }
}
