package com.arcadiadevs.viora.platform.profiles.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.ProfileResourceAssembler;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.UpdateContactProfileCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the profile of the authenticated user.
 *
 * <p>The profile is created at sign-up, so there is no endpoint to create it. Every endpoint is reachable only by the
 * owner of the profile: method security compares the path's {@code userId} with the current user (the session, or
 * the demo producer in the demo profile). Another user's {@code userId} answers 403, and no user at all answers 401.
 * Each endpoint carries its own annotation, because a call from one endpoint to another inside this class does not
 * go through the security proxy.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Profiles", description = "Endpoints for user civil profile and contact management")
public class ProfileController {

    /** Only the owner of the profile named in the path, as resolved by {@code CurrentUserProvider}. */
    private static final String OWNER_ONLY = "@currentUserProvider.isCurrentUser(#userId)";

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    /**
     * Creates the controller.
     *
     * @param profileCommandService profile use cases
     * @param profileQueryService   profile queries
     * @throws IllegalArgumentException if any dependency is null
     */
    public ProfileController(
            ProfileCommandService profileCommandService,
            ProfileQueryService profileQueryService
    ) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        if (profileQueryService == null) {
            throw new IllegalArgumentException("profile.query_service.null");
        }
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
    }

    /**
     * Retrieves the profile of the authenticated user.
     *
     * @param userId the user account identifier; it must be the authenticated user
     * @return 200 with the profile, 403 for another user's profile, 404 if the user has no profile
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get own user profile",
            description = "Retrieves the profile of the authenticated user. Another user's id answers 403.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize(OWNER_ONLY)
    public ResponseEntity<?> getProfileByUserId(@PathVariable String userId) {
        var query = new GetProfileByUserIdQuery(userId);
        return ResponseEntityAssembler.toResponseEntityFromResult(profileQueryService.handle(query),
                ProfileResourceAssembler::toResource, HttpStatus.OK);
    }

    /**
     * Updates the full name, country and phone number of the authenticated user's profile.
     *
     * @param userId   the user account identifier; it must be the authenticated user
     * @param resource the new contact details
     * @return 200 with the updated profile, or 403 for another user's profile
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update own user profile",
            description = "Updates full name, country, and verified phone number of the authenticated user. Another user's id answers 403.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize(OWNER_ONLY)
    public ResponseEntity<?> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        var command = UpdateContactProfileCommandFromResourceAssembler.toCommandFromResource(userId, resource);
        var result = profileCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(result, ProfileResourceAssembler::toResource, HttpStatus.OK);
    }

    /**
     * Same as {@link #updateProfile}: updates the authenticated user's contact details.
     *
     * @param userId   the user account identifier; it must be the authenticated user
     * @param resource the new contact details
     * @return 200 with the updated profile, or 403 for another user's profile
     */
    @PatchMapping("/{userId}")
    @Operation(summary = "Partial update of own user profile",
            description = "Updates the contact information of the authenticated user. Another user's id answers 403.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize(OWNER_ONLY)
    public ResponseEntity<?> patchProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        return updateProfile(userId, resource);
    }
}
