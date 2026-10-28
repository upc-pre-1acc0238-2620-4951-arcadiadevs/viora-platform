package com.arcadiadevs.viora.platform.iam.infrastructure.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/**
 * Limits the IAM requests that cost a password hash or an email (sign-in, sign-up, codes, reset links) per client
 * address, so one client cannot slow the whole platform down or burn the email quota.
 *
 * <p>The limit is generous, because many phones can share one public address. The per-account limits of
 * {@code AuthAttemptLimiter} are the ones that stop a password from being guessed. Renewing a session is left out:
 * the app does it on its own every few minutes.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRequestRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/v1/auth/";
    private static final String REFRESH_PATH = AUTH_PREFIX + "refresh-tokens";

    private final SlidingWindowCounter requests;
    private final Clock clock;

    /**
     * Creates the filter.
     *
     * @param maxRequests   IAM requests allowed per client address inside the window
     * @param windowMinutes length of the window, in minutes
     * @param clock         application clock
     */
    public AuthRequestRateLimitFilter(
            @Value("${viora.iam.rate-limit.max-requests-per-address:30}") int maxRequests,
            @Value("${viora.iam.rate-limit.window-minutes:5}") long windowMinutes,
            Clock clock
    ) {
        this.requests = new SlidingWindowCounter(maxRequests, Duration.ofMinutes(windowMinutes));
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !path.startsWith(AUTH_PREFIX) || path.equals(REFRESH_PATH);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var wait = requests.tryAcquire(clientAddress(request), clock.instant());
        if (wait.isPresent()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(wait.getAsLong()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many requests from this address. Try again in " + wait.getAsLong()
                    + " seconds.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Render puts the address of the client first in {@code X-Forwarded-For}; without it, the peer address. */
    private static String clientAddress(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
