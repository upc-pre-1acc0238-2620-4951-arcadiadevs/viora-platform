package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.RefreshToken;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.*;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public AuthController(
            UserAccountCommandService userAccountCommandService,
            UserAccountQueryService userAccountQueryService,
            PasswordEncoder passwordEncoder,
            BearerTokenService tokenService,
            ApplicationEventPublisher eventPublisher
    ) {
        if (userAccountCommandService == null) throw new IllegalArgumentException("user.command_service.null");
        if (userAccountQueryService == null) throw new IllegalArgumentException("user.query_service.null");
        if (passwordEncoder == null) throw new IllegalArgumentException("password.encoder.null");
        if (tokenService == null) throw new IllegalArgumentException("jwt.provider.null");
        if (eventPublisher == null) throw new IllegalArgumentException("event.publisher.null");

        this.userAccountCommandService = userAccountCommandService;
        this.userAccountQueryService = userAccountQueryService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Register user account", description = "Creates a new user account and optionally formalizes profile details for mobile onboarding", security = {})
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        String encodedPassword = passwordEncoder.encode(resource.password());
        var command = com.arcadiadevs.viora.platform.iam.interfaces.rest.transform.RegisterUserAccountCommandFromResourceAssembler
                .toCommandFromResource(resource, encodedPassword);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<String, ApplicationError> success -> {
                String userId = success.value();

                // If onboarding profile data is present, publish event for downstream Profiles context
                if (resource.profile() != null) {
                    eventPublisher.publishEvent(new UserSignedUpWithProfileEvent(
                            userId,
                            resource.profile().fullName(),
                            resource.profile().country(),
                            resource.profile().phoneNumber()
                    ));
                }

                String accessToken = tokenService.generateAccessToken(userId, resource.email(), resource.role());
                String refreshToken = RefreshToken.issue().token();
                var sessionResource = new AuthenticatedSessionResource(
                        accessToken,
                        refreshToken,
                        "Bearer",
                        tokenService.getAccessTokenValiditySeconds(),
                        userId,
                        resource.email(),
                        resource.role()
                );
                yield ResponseEntity.status(HttpStatus.CREATED).body(sessionResource);
            }
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Authenticate user credentials", description = "Validates credentials and returns JWT access and refresh token pair", security = {})
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        var query = new GetUserAccountByEmailQuery(resource.email());
        var optionalAccount = userAccountQueryService.handle(query);
        if (optionalAccount.isEmpty()) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.credentials.invalid")
            );
        }

        var account = optionalAccount.get();
        if (!passwordEncoder.matches(resource.password(), account.snapshot().passwordHash().value())) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.forbidden("user", "user.credentials.invalid")
            );
        }

        var snapshot = account.snapshot();
        String accessToken = tokenService.generateAccessToken(
                snapshot.id().userId(),
                snapshot.email().value(),
                snapshot.role().name()
        );
        String refreshToken = RefreshToken.issue().token();
        var sessionResource = new AuthenticatedSessionResource(
                accessToken,
                refreshToken,
                "Bearer",
                tokenService.getAccessTokenValiditySeconds(),
                snapshot.id().userId(),
                snapshot.email().value(),
                snapshot.role().name()
        );

        return ResponseEntity.ok(sessionResource);
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
    @Operation(summary = "Reset password using token", description = "Consumes single-use recovery token and updates account password", security = {})
    public ResponseEntity<?> resetPassword(
            @PathVariable String token,
            @Valid @RequestBody ResetPasswordResource resource
    ) {
        String newEncodedPassword = passwordEncoder.encode(resource.newPassword());
        var command = new ResetUserPasswordCommand(token, newEncodedPassword);
        var result = userAccountCommandService.handle(command);

        return switch (result) {
            case Result.Success<Void, ApplicationError> ignored -> ResponseEntity.noContent().build();
            case Result.Failure<Void, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }
}
