package com.arcadiadevs.viora.platform.shared.infrastructure.security;

import com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpringSecurityCurrentUserProvider Unit Tests")
class SpringSecurityCurrentUserProviderTest {

    private static final String DEMO_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private final SpringSecurityCurrentUserProvider provider = new SpringSecurityCurrentUserProvider(false, DEMO_USER_ID);
    private final SpringSecurityCurrentUserProvider demoProvider = new SpringSecurityCurrentUserProvider(true, DEMO_USER_ID);

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("currentUserId is empty when no authentication is present")
    void emptyWhenNoAuthentication() {
        assertThat(provider.currentUserId()).isEmpty();
    }

    @Test
    @DisplayName("currentUserId is empty for the anonymous authentication")
    void emptyWhenAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(provider.currentUserId()).isEmpty();
    }

    @Test
    @DisplayName("currentUserId is empty when the principal name is blank")
    void emptyWhenNameIsBlank() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("   ", null, List.of()));

        assertThat(provider.currentUserId()).isEmpty();
    }

    @Test
    @DisplayName("currentUserId returns the trimmed authenticated user id")
    void returnsTrimmedUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(" user-1 ", null, List.of()));

        assertThat(provider.currentUserId()).contains("user-1");
    }

    @Test
    @DisplayName("requireCurrentUserId fails when unauthenticated and demo mode is disabled")
    void requireFailsWhenUnauthenticated() {
        assertThatThrownBy(provider::requireCurrentUserId)
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessage("user.unauthenticated");
    }

    @Test
    @DisplayName("requireCurrentUserId falls back to the demo user when demo mode is enabled")
    void requireFallsBackInDemoMode() {
        assertThat(demoProvider.requireCurrentUserId()).isEqualTo(DEMO_USER_ID);
    }

    @Test
    @DisplayName("requireCurrentUserId prefers the authenticated user over the demo fallback")
    void requirePrefersAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("real-user", null, List.of()));

        assertThat(demoProvider.requireCurrentUserId()).isEqualTo("real-user");
    }

    @Test
    @DisplayName("isCurrentUser compares with the authenticated user, ignoring case and surrounding spaces")
    void isCurrentUserComparesWithAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of()));

        assertThat(provider.isCurrentUser(" USER-1 ")).isTrue();
        assertThat(provider.isCurrentUser("user-2")).isFalse();
        assertThat(provider.isCurrentUser(null)).isFalse();
    }

    @Test
    @DisplayName("isCurrentUser uses the demo user in demo mode, and requires a user otherwise")
    void isCurrentUserWithoutAuthentication() {
        assertThat(demoProvider.isCurrentUser(DEMO_USER_ID)).isTrue();
        assertThat(demoProvider.isCurrentUser("user-2")).isFalse();
        assertThatThrownBy(() -> provider.isCurrentUser("user-1"))
                .isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    @DisplayName("Rejects demo mode without a demo user id")
    void rejectsDemoModeWithoutUserId() {
        assertThatThrownBy(() -> new SpringSecurityCurrentUserProvider(true, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("security.demo.user_id.blank");
    }
}
