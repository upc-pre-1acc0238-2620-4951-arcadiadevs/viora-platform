package com.arcadiadevs.viora.platform.iam.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthRequestRateLimitFilterTest {

    private final AuthRequestRateLimitFilter filter =
            new AuthRequestRateLimitFilter(2, 5, Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void refusesAnAddressOnceItSpentItsRequests() throws Exception {
        assertEquals(200, send("POST", "/api/v1/auth/sign-in", "203.0.113.7"));
        assertEquals(200, send("POST", "/api/v1/auth/sign-up", "203.0.113.7"));

        var response = response("POST", "/api/v1/auth/password-reset-requests", "203.0.113.7");

        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertEquals(200, send("POST", "/api/v1/auth/sign-in", "198.51.100.4"));
    }

    @Test
    void leavesSessionRenewalAndOtherRoutesAlone() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("POST", "/api/v1/auth/refresh-tokens", "203.0.113.9"));
            assertEquals(200, send("GET", "/api/v1/plots", "203.0.113.9"));
        }
    }

    private int send(String method, String path, String address) throws Exception {
        return response(method, path, address).getStatus();
    }

    private MockHttpServletResponse response(String method, String path, String address) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.addHeader("X-Forwarded-For", address + ", 10.0.0.1");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
