package com.arcadiadevs.viora.platform.shared.application.security;

/**
 * Raised when an operation needs an authenticated user and the request carries none.
 *
 * <p>Mapped to {@code 401 Unauthorized} by the global exception handler.</p>
 */
public class AuthenticationRequiredException extends RuntimeException {

    /** i18n key describing the missing authentication. */
    public static final String MESSAGE_KEY = "user.unauthenticated";

    /**
     * Creates the exception with the standard i18n key.
     */
    public AuthenticationRequiredException() {
        super(MESSAGE_KEY);
    }
}
