package com.arcadiadevs.viora.platform.shared.application.security;

import java.util.Optional;

/**
 * Port resolving the identity of the user behind the current request.
 *
 * <p>REST controllers depend on this port instead of reading the security framework directly, so the way the
 * identity is obtained (JWT today, a demo fallback in the {@code demo} profile) stays in infrastructure.</p>
 */
public interface CurrentUserProvider {

    /**
     * Resolves the current authenticated user identifier.
     *
     * @return the user identifier, or empty when nobody is authenticated
     */
    Optional<String> currentUserId();

    /**
     * Resolves the current user identifier or fails when nobody is authenticated.
     *
     * @return the user identifier
     * @throws AuthenticationRequiredException if no user is authenticated and no demo fallback applies
     */
    String requireCurrentUserId();

    /**
     * Checks whether the given user identifier is the current user, so a request can only reach the user's own
     * resources. Meant for method security, as in {@code @PreAuthorize("@currentUserProvider.isCurrentUser(#userId)")}.
     *
     * @param userId the user identifier the request is about
     * @return true if it is the current user; false otherwise
     * @throws AuthenticationRequiredException if no user is authenticated and no demo fallback applies
     */
    boolean isCurrentUser(String userId);
}
