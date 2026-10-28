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

    private final SpringSecurityCurrentUserProvider provider = new SpringSecurityCurrentUserProvider();

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
    @DisplayName("requireCurrentUserId fails when unauthenticated")
    void requireFailsWhenUnauthenticated() {
        assertThatThrownBy(provider::requireCurrentUserId)
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessage("user.unauthenticated");
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
    @DisplayName("isCurrentUser requires an authenticated user")
    void isCurrentUserWithoutAuthentication() {
        assertThatThrownBy(() -> provider.isCurrentUser("user-1"))
                .isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    @DisplayName("requireCurrentUserId requires an authenticated user")
    void requireCurrentUserIdRequiresAUser() {
        assertThatThrownBy(provider::requireCurrentUserId)
                .isInstanceOf(AuthenticationRequiredException.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of()));
        assertThat(provider.requireCurrentUserId()).isEqualTo("user-1");
    }

}
