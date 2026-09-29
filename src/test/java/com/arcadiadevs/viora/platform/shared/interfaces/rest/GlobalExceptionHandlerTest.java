package com.arcadiadevs.viora.platform.shared.interfaces.rest;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void handleRuntimeExceptionUsesLocalizedUnexpectedMessageAndSanitizesTraces() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

        var handler = new GlobalExceptionHandler();
        var response = handler.handleRuntimeException(new RuntimeException("secret-internal-database-failure"));
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("Ocurrio un error inesperado.", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/unexpected-error", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
        assertEquals("UNEXPECTED_ERROR", problemDetail.getProperties().get("code"));
        assertFalse(String.valueOf(problemDetail.getDetail()).contains("secret-internal-database-failure"));
    }

    @Test
    void handleIllegalArgumentExceptionReturnsValidationError() {
        var handler = new GlobalExceptionHandler();
        var response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Identifier must be a valid positive number")
        );
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", problemDetail.getTitle());
        assertEquals("Identifier must be a valid positive number", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/validation-error", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleResourceNotFoundExceptionReturnsNotFound() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        var handler = new GlobalExceptionHandler();
        var response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Item", 42L)
        );
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not Found", problemDetail.getTitle());
        assertTrue(problemDetail.getDetail().contains("Resource Item with id 42 was not found."));
        assertEquals("https://api.viora.com/errors/resource-not-found", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleResourceNotFoundExceptionReturnsNotFoundInSpanish() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        var handler = new GlobalExceptionHandler();
        var response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Item", 42L)
        );
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not Found", problemDetail.getTitle());
        assertTrue(problemDetail.getDetail().contains("No se encontro el recurso Item con id 42."));
        assertEquals("https://api.viora.com/errors/resource-not-found", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleBusinessRuleExceptionReturnsUnprocessableEntity() {
        var handler = new GlobalExceptionHandler();
        var response = handler.handleBusinessRuleException(
                new BusinessRuleException("OperationNotAllowed", "The requested operation violates business constraints")
        );
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(422, response.getStatusCode().value());
        assertEquals("Unprocessable Content", problemDetail.getTitle());
        assertEquals("Business rule 'OperationNotAllowed' violated: The requested operation violates business constraints", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/business-rule-violation", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }

    @Test
    void handleDataIntegrityViolationReturnsConflict() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        var handler = new GlobalExceptionHandler();
        var response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Unique constraint violation")
        );
        var problemDetail = Objects.requireNonNull((ProblemDetail) response.getBody());

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", problemDetail.getTitle());
        assertEquals("The requested operation conflicts with existing data.", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/resource-conflict", problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties());
        assertNotNull(problemDetail.getProperties().get("timestamp"));
    }
}

