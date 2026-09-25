package com.arcadiadevs.viora.platform.shared.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Assembler for converting application errors to standardized RFC 7807 ProblemDetail HTTP responses.
 */
@NullMarked
public final class ErrorResponseAssembler {
    private static final String MESSAGES_BASENAME = "messages";
    private static final String DEFAULT_TYPE_PREFIX = "https://api.viora.com/errors/";

    private ErrorResponseAssembler() {
    }

    /**
     * Converts an {@link ApplicationError} into an RFC 7807 {@link ProblemDetail} structure.
     *
     * @param error the application error to transform
     * @return a structured {@link ProblemDetail} containing type, title, status, detail, instance, and timestamp
     */
    public static ProblemDetail toProblemDetail(ApplicationError error) {
        HttpStatusCode status = toStatusFromErrorCode(error.code());
        String detailMessage = (error.details() != null && !error.details().isBlank())
                ? error.details()
                : toLocalizedMessageFromApplicationError(error);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        problemDetail.setType(toTypeUriFromErrorCode(error.code()));
        problemDetail.setTitle(toTitleFromStatus(status));
        problemDetail.setInstance(resolveCurrentRequestUri());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("code", error.code());
        return problemDetail;
    }

    /**
     * Maps an {@link ApplicationError} to an appropriate HTTP {@link ResponseEntity} containing a {@link ProblemDetail}.
     * Automatically selects the correct HTTP status code based on the error code.
     *
     * @param error the ApplicationError to map
     * @return a ResponseEntity with the appropriate HTTP status and ProblemDetail resource
     */
    public static ResponseEntity<ProblemDetail> toErrorResponseFromApplicationError(ApplicationError error) {
        ProblemDetail problemDetail = toProblemDetail(error);
        return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
    }

    private static URI toTypeUriFromErrorCode(String errorCode) {
        String slug = errorCode.toLowerCase(Locale.ROOT).replace('_', '-');
        return URI.create(DEFAULT_TYPE_PREFIX + slug);
    }

    private static String toTitleFromStatus(HttpStatusCode status) {
        if (status instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return switch (status.value()) {
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }

    private static URI resolveCurrentRequestUri() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String uri = request.getRequestURI();
                if (uri != null && !uri.isBlank()) {
                    return URI.create(uri);
                }
            }
        } catch (Exception ignored) {
            // Fallback when request attributes are unavailable
        }
        return URI.create("about:blank");
    }

    private static String toLocalizedMessageFromApplicationError(ApplicationError error) {
        String specificKey = toSpecificMessageKeyFromErrorCode(error.code());
        String specificMessage = toLocalizedMessageOrNull(specificKey, error.details(), toEntityNameFromErrorCode(error.code()));
        if (specificMessage != null) {
            return specificMessage;
        }

        if (error.message() != null && !error.message().isBlank()) {
            return error.message();
        }

        String fallbackKey = toMessageKeyFromErrorCode(error.code());
        return toLocalizedMessageWithFallback(
                fallbackKey,
                "An error occurred.",
                error.details(),
                toEntityNameFromErrorCode(error.code())
        );
    }

    private static String toSpecificMessageKeyFromErrorCode(String errorCode) {
        return "error.%s.message".formatted(errorCode.toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    private static String toMessageKeyFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "VALIDATION_ERROR" -> "error.validation.message";
            case "BUSINESS_RULE_VIOLATION" -> "error.business-rule.message";
            case "UNEXPECTED_ERROR" -> "error.unexpected.message";
            case String s when s.endsWith("_NOT_FOUND") -> "error.not-found.message";
            case String s when s.endsWith("_CONFLICT") -> "error.conflict.message";
            default -> "error.generic.message";
        };
    }

    private static String toEntityNameFromErrorCode(String errorCode) {
        if (errorCode.endsWith("_NOT_FOUND")) {
            return errorCode.replace("_NOT_FOUND", "").toLowerCase(Locale.ROOT);
        }
        if (errorCode.endsWith("_CONFLICT")) {
            return errorCode.replace("_CONFLICT", "").toLowerCase(Locale.ROOT);
        }
        return "resource";
    }

    private static String toLocalizedMessageOrNull(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, locale);
            if (!bundle.containsKey(key)) {
                return null;
            }
            String template = bundle.getString(key);
            return MessageFormat.format(template, args);
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    private static String toLocalizedMessageWithFallback(String key, String fallback, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, locale);
            if (!bundle.containsKey(key)) {
                return fallback;
            }
            String template = bundle.getString(key);
            return MessageFormat.format(template, args);
        } catch (MissingResourceException ex) {
            return fallback;
        }
    }

    /**
     * Determines the appropriate HTTP status code for a given error code.
     *
     * @param errorCode the error code string (e.g., "PROFILE_NOT_FOUND", "VALIDATION_ERROR")
     * @return the corresponding HttpStatus
     */
    public static HttpStatusCode toStatusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case String s when s.endsWith("_FORBIDDEN") -> HttpStatus.FORBIDDEN;
            case String s when s.endsWith("_NOT_FOUND") -> HttpStatus.NOT_FOUND;
            case String s when s.endsWith("_PRECONDITION_FAILED") -> HttpStatus.PRECONDITION_FAILED;
            case "BUSINESS_RULE_VIOLATION" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case String s when s.endsWith("_CONFLICT") -> HttpStatus.CONFLICT;
            case "UNEXPECTED_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
