package com.arcadiadevs.viora.platform.subscription.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedemptionRequestRateLimitFilterTest {

    private final RedemptionRequestRateLimitFilter filter = new RedemptionRequestRateLimitFilter(2, 60,
            Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void refusesAnAddressOnceItSpentItsRedemptionsWhicheverCodeKindItTries() throws Exception {
        assertEquals(200, send("/api/v1/cooperative-code-redemptions", "203.0.113.7"));
        assertEquals(200, send("/api/v1/quota-extension-redemptions", "203.0.113.7"));

        assertEquals(429, send("/api/v1/cooperative-code-redemptions", "203.0.113.7"));
        assertEquals(200, send("/api/v1/cooperative-code-redemptions", "198.51.100.4"));
    }

    @Test
    void leavesOtherRoutesAlone() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("/api/v1/subscriptions", "203.0.113.9"));
        }
    }

    private int send(String path, String address) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.addHeader("X-Forwarded-For", address);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
