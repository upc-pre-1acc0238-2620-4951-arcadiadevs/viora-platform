package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;

/**
 * Authentication entry point handling unauthorized requests within the Spring Security filter chain. It answers
 * the same RFC 7807 problem detail as every other error of the API.
 */
@Component
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {
    /** Message key the assembler localizes into the detail of the problem. */
    private static final String MESSAGE_KEY = "error.unauthorized";

    private final LocaleResolver localeResolver;
    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    public UnauthorizedRequestHandlerEntryPoint(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        // The filter chain runs before the dispatcher resolves the locale, so it is resolved here the same way
        var previousLocale = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        try {
            var problemDetail = ErrorResponseAssembler.toProblemDetail(ApplicationError.unauthorized("user", MESSAGE_KEY));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            converter.write(problemDetail, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
        } finally {
            LocaleContextHolder.setLocaleContext(previousLocale);
        }
    }
}
