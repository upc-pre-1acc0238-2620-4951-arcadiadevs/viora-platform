package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.RefreshToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.*;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication, registration, session renewal, and password recovery endpoints.
 */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Identity & Access Management", description = "Endpoints for authentication, credentials, and session lifecycle")
public class AuthController {

    private final UserAccountCommandService userAccountCommandService;
    private final UserAccountQueryService userAccountQueryService;
    private final PasswordEncoder passwordEncoder;
    private final BearerTokenService tokenService;

    public AuthController(
            UserAccountCommandService userAccountCommandService,
            UserAccountQueryService userAccountQueryService,
            PasswordEncoder passwordEncoder,
            BearerTokenService tokenService
    ) {
        if (userAccountCommandService == null) throw new IllegalArgumentException("user.command_service.null");
        if (userAccountQueryService == null) throw new IllegalArgumentException("user.query_service.null");
        if (passwordEncoder == null) throw new IllegalArgumentException("password.encoder.null");
        if (tokenService == null) throw new IllegalArgumentException("jwt.provider.null");

        this.userAccountCommandService = userAccountCommandService;
        this.userAccountQueryService = userAccountQueryService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up a user account",
            description = "Creates an account pending email verification, with its onboarding profile, and emails a 6-digit verification code. No session is issued until the email is verified. Signing up again with the email of a pending account reuses it with a new code.",
            security = {})
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        String encodedPassword = passwordEncoder.encode(resource.password());
        var command = com.arcadiadevs.viora.platform.iam.interfaces.rest.transform.RegisterUserAccountCommandFromResourceAssembler
                .toCommandFromResource(resource, encodedPassword);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<String, ApplicationError> success -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new SignUpAcceptedResource(success.value(), resource.email(),
                            UserAccountStatus.PENDING_VERIFICATION.name()));
            case Result.Failure<String, ApplicationError> failure -> ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/email-verifications")
    @Operation(summary = "Verify the account email",
            description = "Checks the 6-digit code sent at sign-up, activates the account and returns the session. A wrong code answers 422 with the remaining attempts; after 3 wrong codes a new one must be requested.",
            security = {})
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailResource resource) {
        var result = userAccountCommandService.handle(new VerifyEmailCommand(resource.email(), resource.code()));

        return switch (result) {
            case Result.Success<String, ApplicationError> ignored ->
                    userAccountQueryService.handle(new GetUserAccountByEmailQuery(resource.email()))
                            .<ResponseEntity<?>>map(account -> ResponseEntity.ok(toSessionResource(account.snapshot())))
                            .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                                    ApplicationError.notFound("user", resource.email())));
            case Result.Failure<String, ApplicationError> failure -> ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
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
            case Result.Failure<Void, ApplicationError> failure -> ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Authenticate user credentials",
            description = "Validates credentials and returns JWT access and refresh token pair. An account whose email is not verified yet answers 403 with code EMAIL_VERIFICATION_FORBIDDEN.",
            security = {})
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        var query = new GetUserAccountByEmailQuery(resource.email());
        var optionalAccount = userAccountQueryService.handle(query);
        if (optionalAccount.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.credentials.invalid")
            );
        }

        var snapshot = optionalAccount.get().snapshot();
        if (!passwordEncoder.matches(resource.password(), snapshot.passwordHash().value())) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.credentials.invalid")
            );
        }
        // Checked after the password, so it only tells the account owner that the email still needs verifying
        if (snapshot.status() != UserAccountStatus.ACTIVE) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("email-verification", "user.email.not_verified")
            );
        }

        return ResponseEntity.ok(toSessionResource(snapshot));
    }

    @PostMapping("/refresh-tokens")
    @Operation(summary = "Renew session tokens", description = "Rotates refresh token and issues a fresh access token", security = {})
    public ResponseEntity<?> refreshTokens(@Valid @RequestBody RefreshTokenResource resource) {
        // Parse refresh token / access token claims
        var claims = tokenService.parseClaims(resource.refreshToken());
        if (claims.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.refresh_token.invalid")
            );
        }

        String userId = claims.get().getSubject();
        String email = claims.get().get("email", String.class);
        String role = claims.get().get("role", String.class);

        String newAccessToken = tokenService.generateAccessToken(userId, email, role);
        String newRefreshToken = RefreshToken.issue().token();
        var sessionResource = new AuthenticatedSessionResource(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                tokenService.getAccessTokenValiditySeconds(),
                userId,
                email,
                role
        );

        return ResponseEntity.ok(sessionResource);
    }

    @PatchMapping("/passwords")
    @Operation(summary = "Change account password", description = "Updates password for currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordResource resource,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.unauthenticated")
            );
        }

        String userId = authentication.getName();
        String newEncodedPassword = passwordEncoder.encode(resource.newPassword());
        var command = new ChangeUserPasswordCommand(userId, resource.currentPassword(), newEncodedPassword);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<String, ApplicationError> ignored -> ResponseEntity.noContent().build();
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/password-reset-tokens")
    @Operation(summary = "Request password reset", description = "Initiates email recovery token delivery via Brevo without revealing email existence", security = {})
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetResource resource) {
        userAccountCommandService.handle(new RequestPasswordResetCommand(resource.email()));
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/password-reset-tokens/{token}")
    @Operation(summary = "Reset password using recovery code", description = "Consumes the single-use 6-digit code sent to the given email and updates the account password; the code is locked after 5 wrong attempts", security = {})
    public ResponseEntity<?> resetPassword(
            @PathVariable String token,
            @Valid @RequestBody ResetPasswordResource resource
    ) {
        String newEncodedPassword = passwordEncoder.encode(resource.newPassword());
        var command = new ResetUserPasswordCommand(resource.email(), token, newEncodedPassword);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<Void, ApplicationError> ignored -> ResponseEntity.noContent().build();
            case Result.Failure<Void, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    private AuthenticatedSessionResource toSessionResource(UserAccountSnapshot snapshot) {
        String accessToken = tokenService.generateAccessToken(
                snapshot.id().userId(),
                snapshot.email().value(),
                snapshot.role().name()
        );
        return new AuthenticatedSessionResource(
                accessToken,
                RefreshToken.issue().token(),
                "Bearer",
                tokenService.getAccessTokenValiditySeconds(),
                snapshot.id().userId(),
                snapshot.email().value(),
                snapshot.role().name()
        );
    }
}
