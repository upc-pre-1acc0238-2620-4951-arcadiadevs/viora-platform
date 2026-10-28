package com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the address of the client behind a request, for the limits applied per address. */
public final class ClientAddress {

    private ClientAddress() {
    }

    /**
     * Render puts the address of the client first in {@code X-Forwarded-For}; without that header, the peer address.
     * A client can forge the header, so these limits only slow abuse down: the per-account ones are the real guard.
     *
     * @param request the request
     * @return the client address
     */
    public static String of(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
