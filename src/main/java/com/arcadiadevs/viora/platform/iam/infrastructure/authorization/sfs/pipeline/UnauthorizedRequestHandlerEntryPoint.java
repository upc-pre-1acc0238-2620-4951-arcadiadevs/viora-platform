package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.arcadiadevs.viora.platform.shared.application.security.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Authentication entry point handling unauthorized requests within the Spring Security filter chain. It hands the
 * request to the same exception resolver the controllers use, so a missing or invalid token answers the RFC 7807
 * {@code ProblemDetail} with its i18n message, like every other error of the API, instead of the container's page.
 */
@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * Creates the entry point.
     *
     * @param exceptionResolver the resolver that applies the global exception handler
     */
    public UnauthorizedRequestHandlerEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) {
        exceptionResolver.resolveException(request, response, null, new AuthenticationRequiredException());
    }
}
