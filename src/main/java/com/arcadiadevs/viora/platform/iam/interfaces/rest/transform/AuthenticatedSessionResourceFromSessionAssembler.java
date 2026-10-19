package com.arcadiadevs.viora.platform.iam.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.AuthenticatedSessionResource;

/**
 * Assembler transforming an {@link AuthenticatedSession} into its {@link AuthenticatedSessionResource}.
 */
public final class AuthenticatedSessionResourceFromSessionAssembler {

    private static final String TOKEN_TYPE = "Bearer";

    private AuthenticatedSessionResourceFromSessionAssembler() {
    }

    /**
     * Converts an opened session into the response resource.
     *
     * @param session the opened session
     * @return the session resource
     */
    public static AuthenticatedSessionResource toResourceFromSession(AuthenticatedSession session) {
        return new AuthenticatedSessionResource(
                session.accessToken(),
                session.refreshToken(),
                TOKEN_TYPE,
                session.accessTokenValidity(),
                session.userId(),
                session.email(),
                session.role()
        );
    }
}
