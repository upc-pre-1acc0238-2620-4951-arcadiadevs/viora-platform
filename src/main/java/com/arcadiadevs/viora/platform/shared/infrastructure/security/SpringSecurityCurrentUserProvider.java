package com.arcadiadevs.viora.platform.shared.infrastructure.security;

import com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link CurrentUserProvider} backed by the Spring Security context populated by the bearer token filter.
 *
 * <p>There is no anonymous fallback: every route outside the public list requires a valid token, and the demo data
 * is reached by signing in with the demo account.</p>
 *
 * <p>It is registered as {@code currentUserProvider} so method security expressions can call it.</p>
 */
@Component("currentUserProvider")
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<String> currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        var name = authentication.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name.trim());
    }

    @Override
    public String requireCurrentUserId() {
        return currentUserId().orElseThrow(AuthenticationRequiredException::new);
    }

    @Override
    public boolean isCurrentUser(String userId) {
        var currentUserId = requireCurrentUserId();
        return userId != null && userId.trim().equalsIgnoreCase(currentUserId);
    }
}
