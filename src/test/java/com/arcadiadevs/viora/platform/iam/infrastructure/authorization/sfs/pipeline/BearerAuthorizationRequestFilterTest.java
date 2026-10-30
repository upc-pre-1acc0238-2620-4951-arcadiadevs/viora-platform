package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BearerAuthorizationRequestFilter Unit Tests")
class BearerAuthorizationRequestFilterTest {

    private final BearerTokenService tokenService = mock(BearerTokenService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final BearerAuthorizationRequestFilter filter = new BearerAuthorizationRequestFilter(
            tokenService, userDetailsService, new UnauthorizedRequestHandlerEntryPoint(new AcceptHeaderLocaleResolver()), "/password-resets");

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String path, String token) {
        var request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        when(tokenService.getBearerTokenFrom(any())).thenReturn(token);
        return request;
    }

    @Test
    @DisplayName("A rejected token on a protected path answers 401 instead of falling back to the demo producer")
    void rejectedTokenAnswers401() throws Exception {
        var request = request("/api/v1/plots", "expired");
        when(tokenService.validateToken("expired")).thenReturn(false);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("A rejected token sent to a public path is ignored")
    void rejectedTokenOnPublicPathIsIgnored() throws Exception {
        for (var path : List.of("/api/v1/auth/sign-in", "/password-resets/abc", "/.well-known/assetlinks.json")) {
            var request = request(path, "expired");
            when(tokenService.validateToken("expired")).thenReturn(false);
            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).as(path).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Without a token the request goes on anonymously")
    void noTokenGoesOn() throws Exception {
        var request = request("/api/v1/plots", null);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("A valid token authenticates its user")
    void validTokenAuthenticates() throws Exception {
        var request = request("/api/v1/plots", "valid");
        when(tokenService.validateToken("valid")).thenReturn(true);
        when(tokenService.getUsernameFromToken("valid")).thenReturn("producer@viora.pe");
        when(userDetailsService.loadUserByUsername("producer@viora.pe"))
                .thenReturn(User.withUsername("producer@viora.pe").password("x").authorities(List.of()).build());
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
