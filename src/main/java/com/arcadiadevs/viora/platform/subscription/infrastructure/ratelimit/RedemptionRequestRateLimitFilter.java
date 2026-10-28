package com.arcadiadevs.viora.platform.subscription.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit.ClientAddress;
import com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit.SlidingWindowCounter;
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
import java.util.Set;

/**
 * Limits the code redemptions per client address, on top of the failed attempts each producer may make.
 *
 * <p>The codes are short, and signing up is free: without this, many accounts from one address could try codes
 * until one works. A real producer redeems one code, maybe two.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RedemptionRequestRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> REDEMPTION_PATHS =
            Set.of("/api/v1/cooperative-code-redemptions", "/api/v1/quota-extension-redemptions");

    private final SlidingWindowCounter requests;
    private final Clock clock;

    /**
     * Creates the filter.
     *
     * @param maxRequests   redemptions allowed per client address inside the window
     * @param windowMinutes length of the window, in minutes
     * @param clock         application clock
     */
    public RedemptionRequestRateLimitFilter(
            @Value("${viora.subscription.redemption.max-requests-per-address:10}") int maxRequests,
            @Value("${viora.subscription.redemption.address-window-minutes:60}") long windowMinutes,
            Clock clock
    ) {
        this.requests = new SlidingWindowCounter(maxRequests, Duration.ofMinutes(windowMinutes));
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !REDEMPTION_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var wait = requests.tryAcquire(ClientAddress.of(request), clock.instant());
        if (wait.isPresent()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(wait.getAsLong()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many code redemptions from this address. Try again in " + wait.getAsLong()
                    + " seconds.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
