package com.arcadiadevs.viora.platform.profiles.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.application.queryservices.ProfileQueryService;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.UpdateProfileResource;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.ProfileResourceAssembler;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform.UpdateContactProfileCommandFromResourceAssembler;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the profile of the authenticated user.
 *
 * <p>The profile is created at sign-up, so there is no endpoint to create it. The path names whose profile is
 * requested and the session names who asks; the profile services allow only the owner, so another user's
 * {@code userId} answers 403, and a request without a session answers 401.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Profiles", description = "Endpoints for user civil profile and contact management")
public class ProfileController {

    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the controller.
     *
     * @param profileCommandService profile use cases
     * @param profileQueryService   profile queries
     * @param currentUserProvider   resolves the authenticated user, who acts on the profile
     * @throws IllegalArgumentException if any dependency is null
     */
    public ProfileController(
            ProfileCommandService profileCommandService,
            ProfileQueryService profileQueryService,
            CurrentUserProvider currentUserProvider
    ) {
        if (profileCommandService == null) {
            throw new IllegalArgumentException("profile.command_service.null");
        }
        if (profileQueryService == null) {
            throw new IllegalArgumentException("profile.query_service.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("security.current_user_provider.null");
        }
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Retrieves the profile of the authenticated user.
     *
     * @param userId the user account identifier; it must be the authenticated user
     * @return 200 with the profile, 403 for another user's profile, 404 if the user has no profile
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get own user profile",
            description = "Retrieves the profile of the authenticated user. Another user's id answers 403 PROFILE_FORBIDDEN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getProfileByUserId(@PathVariable String userId) {
        var query = new GetProfileByUserIdQuery(userId, currentUserProvider.requireCurrentUserId());
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
            description = "Updates full name, country, and verified phone number of the authenticated user. Another user's id answers 403 PROFILE_FORBIDDEN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        var command = UpdateContactProfileCommandFromResourceAssembler.toCommandFromResource(userId,
                currentUserProvider.requireCurrentUserId(), resource);
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
            description = "Updates the contact information of the authenticated user. Another user's id answers 403 PROFILE_FORBIDDEN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> patchProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        return updateProfile(userId, resource);
    }
}
