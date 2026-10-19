package com.arcadiadevs.viora.platform.shared.infrastructure.security;

import com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link CurrentUserProvider} backed by the Spring Security context populated by the bearer token filter.
 *
 * <p>When demo mode is enabled ({@code viora.security.demo.enabled=true}, set by the {@code demo} profile) and no
 * user is authenticated, it falls back to the seeded demo producer so the demo data stays reachable without
 * signing in. An authenticated user always takes precedence over the fallback.</p>
 *
 * <p>It is registered as {@code currentUserProvider} so method security expressions can call it.</p>
 */
@Component("currentUserProvider")
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    private final boolean demoModeEnabled;
    private final String demoUserId;

    /**
     * Creates the provider.
     *
     * @param demoModeEnabled whether unauthenticated requests fall back to the demo user
     * @param demoUserId      the seeded demo producer identifier used as fallback
     * @throws IllegalArgumentException if demo mode is enabled without a demo user identifier
     */
    public SpringSecurityCurrentUserProvider(
            @Value("${viora.security.demo.enabled:false}") boolean demoModeEnabled,
            @Value("${viora.security.mock.default-producer-id:}") String demoUserId
    ) {
        if (demoModeEnabled && (demoUserId == null || demoUserId.isBlank())) {
            throw new IllegalArgumentException("security.demo.user_id.blank");
        }
        this.demoModeEnabled = demoModeEnabled;
        this.demoUserId = demoUserId == null ? "" : demoUserId.trim();
    }

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
        return currentUserId()
                .or(() -> demoModeEnabled ? Optional.of(demoUserId) : Optional.empty())
                .orElseThrow(AuthenticationRequiredException::new);
    }

    @Override
    public boolean isCurrentUser(String userId) {
        var currentUserId = requireCurrentUserId();
        return userId != null && userId.trim().equalsIgnoreCase(currentUserId);
    }
}
