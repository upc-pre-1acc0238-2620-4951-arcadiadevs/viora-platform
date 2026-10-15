package com.arcadiadevs.viora.platform.profiles.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.CreateProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.UpdateContactProfileCommand;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.CreateProfileResource;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.ProfileResource;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.ProfileResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller exposing endpoints for user profiles management.
 */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Profiles", description = "Endpoints for user civil profile and contact management")
public class ProfileController {

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    public ProfileController(
            ProfileCommandService profileCommandService,
            ProfileQueryService profileQueryService
    ) {
        if (profileCommandService == null) throw new IllegalArgumentException("profile.command_service.null");
        if (profileQueryService == null) throw new IllegalArgumentException("profile.query_service.null");

        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
    }

    @PostMapping
    @Operation(summary = "Create user profile", description = "Initializes contact and civil profile for an existing user account")
    public ResponseEntity<?> createProfile(@Valid @RequestBody CreateProfileResource resource) {
        var command = com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.CreateProfileCommandFromResourceAssembler
                .toCommandFromResource(resource);
        var result = profileCommandService.handle(command);

        return switch (result) {
            case Result.Success<String, ApplicationError> success -> {
                var query = new GetProfileByUserIdQuery(resource.userId());
                var profile = profileQueryService.handle(query).orElseThrow();
                var responseResource = ProfileResourceAssembler.toResource(profile);
                yield ResponseEntity.created(URI.create("/api/v1/profiles/" + resource.userId())).body(responseResource);
            }
            case Result.Failure<String, ApplicationError> failure ->
                    ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
        };
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile", description = "Retrieves user profile by user account identifier")
    public ResponseEntity<?> getProfileByUserId(@PathVariable String userId) {
        try {
            var query = new GetProfileByUserIdQuery(userId);
            var optionalProfile = profileQueryService.handle(query);
            if (optionalProfile.isEmpty()) {
                return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("profile", userId)
                );
            }
            return ResponseEntity.ok(ProfileResourceAssembler.toResource(optionalProfile.get()));
        } catch (IllegalArgumentException ex) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("userId", ex.getMessage())
            );
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile", description = "Updates full name, country, and verified phone number")
    public ResponseEntity<?> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        var command = com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.UpdateContactProfileCommandFromResourceAssembler
                .toCommandFromResource(userId, resource);
        var result = profileCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(result, ProfileResourceAssembler::toResource, HttpStatus.OK);
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Partial update of user profile", description = "Updates profile contact information")
    public ResponseEntity<?> patchProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        return updateProfile(userId, resource);
    }
}
