package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter intercepting incoming HTTP requests to validate Bearer tokens and set authentication context.
 */
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    /** Paths that never need a session, so a stale token sent to them is simply ignored. */
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/", "/.well-known/", "/v3/api-docs", "/swagger-ui", "/email/", "/error");

    private final BearerTokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint unauthorizedHandler;
    private final String passwordResetPath;

    /**
     * Creates the filter.
     *
     * @param tokenService        reads and validates bearer tokens
     * @param userDetailsService  loads the user named by a token
     * @param unauthorizedHandler answers 401 when a presented token is rejected
     * @param passwordResetPath   path of the password reset links, public like the IAM endpoints
     */
    public BearerAuthorizationRequestFilter(
            BearerTokenService tokenService,
            @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            AuthenticationEntryPoint unauthorizedHandler,
            String passwordResetPath
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.passwordResetPath = passwordResetPath;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = null;
        boolean authenticated = false;
        try {
            token = tokenService.getBearerTokenFrom(request);
            authenticated = token != null && authenticate(token, request);
        } catch (Exception ignored) {
            // A malformed token is handled below like any other rejected one
        }

        // A token that was presented but does not authenticate (expired, tampered, issued before the last password
        // change) answers 401, so the app renews the session. On a public route it is ignored instead.
        if (token != null && !authenticated && !isPublic(request)) {
            SecurityContextHolder.clearContext();
            unauthorizedHandler.commence(request, response, new BadCredentialsException("security.bearer_token.rejected"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticate(String token, HttpServletRequest request) {
        if (!tokenService.validateToken(token)) {
            return false;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return true;
        }
        String username = tokenService.getUsernameFromToken(token);
        if (username == null) {
            return false;
        }
        var userDetails = userDetailsService.loadUserByUsername(username);
        // A token issued before the last password change carries an older credentials version
        if (userDetails instanceof UserDetailsImpl account
                && tokenService.extractCredentialsVersion(token).orElse(0L) != account.getCredentialsVersion()) {
            return false;
        }
        var authentication = UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith(passwordResetPath + "/") || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
