package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Handles the requests the Spring Security filter chain refuses because of the role of the account. It hands them to
 * the same exception resolver the controllers use, so they answer the RFC 7807 {@code ProblemDetail} with its i18n
 * message, like every other error of the API, instead of the container's page.
 */
@Component
public class ForbiddenRequestHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    /**
     * Creates the handler.
     *
     * @param exceptionResolver the resolver that applies the global exception handler
     */
    public ForbiddenRequestHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) {
        exceptionResolver.resolveException(request, response, null, accessDeniedException);
    }
}
