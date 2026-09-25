package com.arcadiadevs.viora.platform.shared.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Global exception handler for REST API.
 * Provides centralized exception handling following the RFC 7807 Problem Details specification,
 * ensuring all unhandled and domain exceptions are mapped to semantic HTTP responses without
 * exposing internal server traces.
 */
@RestControllerAdvice
@NullMarked
@Slf4j
public class GlobalExceptionHandler {
    private static final String MESSAGES_BASENAME = "messages";

    /**
     * Handles validation exceptions from Spring's request body validation (@Valid).
     * Maps validation failures to a standardized RFC 7807 ProblemDetail response.
     *
     * @param ex the validation exception from @Valid binding
     * @return ProblemDetail response with BAD_REQUEST status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        var validationPrefix = resolveMessageOrDefault("validation.field.prefix", "Field");
        var errorDetails = fieldErrors.isEmpty()
                ? resolveMessageOrDefault("validation.request.failed", "Request validation failed")
                : fieldErrors.stream()
                .map(error -> "%s %s: %s".formatted(
                        validationPrefix,
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .reduce((a, b) -> a + "; " + b)
                .orElse(resolveMessageOrDefault("validation.request.failed", "Request validation failed"));

        var applicationError = ApplicationError.validationError("request-body", errorDetails);
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles domain resource not found exceptions.
     *
     * @param ex the resource not found exception
     * @return ProblemDetail response with NOT_FOUND status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        var applicationError = ApplicationError.notFound(
                "resource",
                ex.getMessage() != null ? ex.getMessage() : resolveMessageOrDefault("error.not-found.message", "Resource not found")
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles domain business rule violation exceptions.
     *
     * @param ex the business rule exception
     * @return ProblemDetail response with UNPROCESSABLE_ENTITY status
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleException(BusinessRuleException ex) {
        var detail = ex.getMessage() != null
                ? resolveMessageOrDefault(ex.getMessage(), ex.getMessage())
                : resolveMessageOrDefault("error.business-rule.message", "Business rule violation");
        var applicationError = ApplicationError.businessRuleViolation("business-rule", detail);
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles domain resource conflict exceptions.
     *
     * @param ex the resource conflict exception
     * @return ProblemDetail response with CONFLICT status
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflictException(ResourceConflictException ex) {
        var detail = ex.getMessage() != null
                ? resolveMessageOrDefault(ex.getMessage(), ex.getMessage())
                : resolveMessageOrDefault("error.conflict.message", "Resource conflict");
        var applicationError = ApplicationError.conflict("resource", detail);
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles malformed JSON, invalid parameter types, and missing parameters.
     *
     * @param ex the request binding exception
     * @return ProblemDetail response with BAD_REQUEST status
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ProblemDetail> handleRequestBindingException(Exception ex) {
        var applicationError = ApplicationError.validationError(
                "request",
                resolveMessageOrDefault("validation.request.failed", "Request validation failed")
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles persistence conflicts such as unique constraint violations.
     *
     * @param ex the persistence exception
     * @return ProblemDetail response with CONFLICT status
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Persistence constraint violation", ex);
        var applicationError = ApplicationError.conflict(
                "resource",
                resolveMessageOrDefault(
                        "error.persistence-conflict.details",
                        "The requested operation conflicts with existing data."
                )
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles invalid request arguments such as malformed UUID path or payload values.
     *
     * @param ex the illegal argument exception
     * @return ProblemDetail response with BAD_REQUEST status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        var detail = ex.getMessage() != null
                ? resolveMessageOrDefault(ex.getMessage(), ex.getMessage())
                : resolveMessageOrDefault("validation.request.failed", "Request validation failed");
        var applicationError = ApplicationError.validationError(
                resolveMessageOrDefault("validation.request.argument", "request-argument"),
                detail
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles unexpected runtime exceptions not caught by specific handlers.
     * Sanitizes response details to prevent internal trace exposure.
     *
     * @param ex the unhandled runtime exception
     * @return ProblemDetail response with INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        var applicationError = ApplicationError.unexpected(
                resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
                resolveMessageOrDefault(
                        "error.unexpected.details",
                        "An unexpected error occurred."
                )
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    /**
     * Handles all other checked and unchecked exceptions not matched by specific handlers.
     * Provides a final fallback for any unexpected exception type without exposing internal traces.
     *
     * @param ex the unhandled exception
     * @return ProblemDetail response with INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        var applicationError = ApplicationError.unexpected(
                resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
                resolveMessageOrDefault(
                        "error.unexpected.details",
                        "An unexpected error occurred."
                )
        );
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }

    private String resolveMessageOrDefault(String key, String defaultValue, Object... args) {
        try {
            var bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, LocaleContextHolder.getLocale());
            if (!bundle.containsKey(key)) {
                return defaultValue;
            }
            return MessageFormat.format(bundle.getString(key), args);
        } catch (MissingResourceException ex) {
            return defaultValue;
        }
    }
}
