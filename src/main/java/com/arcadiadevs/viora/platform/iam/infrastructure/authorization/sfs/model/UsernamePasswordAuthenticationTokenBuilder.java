package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.model;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * Factory builder constructing {@link UsernamePasswordAuthenticationToken} instances for authenticated principals.
 */
public class UsernamePasswordAuthenticationTokenBuilder {

    /**
     * Builds an authentication token for the given principal and request.
     *
     * @param principal authenticated user details
     * @param request current HTTP servlet request
     * @return constructed UsernamePasswordAuthenticationToken
     */
    public static UsernamePasswordAuthenticationToken build(UserDetails principal, HttpServletRequest request) {
        var token = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return token;
    }
}
