package com.arcadiadevs.viora.platform.shared.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ErrorResponseAssemblerTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void toProblemDetail_whenConflictWithI18nKeyInSpanish_resolvesSpanishDetail() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

        var error = ApplicationError.conflict("plot", "plot.name.duplicate");
        var problemDetail = ErrorResponseAssembler.toProblemDetail(error);

        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        assertEquals("Conflict", problemDetail.getTitle());
        assertEquals("Ya existe una parcela con este nombre para el productor", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/plot-conflict", problemDetail.getType().toString());
        assertEquals("PLOT_CONFLICT", problemDetail.getProperties().get("code"));
    }

    @Test
    void toProblemDetail_whenConflictWithI18nKeyInEnglish_resolvesEnglishDetail() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        var error = ApplicationError.conflict("plot", "plot.name.duplicate");
        var problemDetail = ErrorResponseAssembler.toProblemDetail(error);

        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
        assertEquals("Conflict", problemDetail.getTitle());
        assertEquals("A plot with this name already exists for the producer", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/plot-conflict", problemDetail.getType().toString());
        assertEquals("PLOT_CONFLICT", problemDetail.getProperties().get("code"));
    }

    @Test
    void toProblemDetail_whenNotFoundWithIdInSpanish_resolvesFormattedSpanishMessage() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

        var error = ApplicationError.notFound("Plot", "50f74a4c-1a91-4fa3-83a9-f227004cb5fc");
        var problemDetail = ErrorResponseAssembler.toProblemDetail(error);

        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("No se encontro el recurso Plot con id 50f74a4c-1a91-4fa3-83a9-f227004cb5fc.", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/plot-not-found", problemDetail.getType().toString());
        assertEquals("PLOT_NOT_FOUND", problemDetail.getProperties().get("code"));
    }

    @Test
    void toProblemDetail_whenNotFoundWithIdInEnglish_resolvesFormattedEnglishMessage() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        var error = ApplicationError.notFound("Plot", "50f74a4c-1a91-4fa3-83a9-f227004cb5fc");
        var problemDetail = ErrorResponseAssembler.toProblemDetail(error);

        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("Resource Plot with id 50f74a4c-1a91-4fa3-83a9-f227004cb5fc was not found.", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/plot-not-found", problemDetail.getType().toString());
        assertEquals("PLOT_NOT_FOUND", problemDetail.getProperties().get("code"));
    }

    @Test
    void toProblemDetail_whenNotFoundWithComplexAggregateInSpanish_resolvesFormattedSpanishMessage() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));

        var error = ApplicationError.notFound("ChillAccumulationTracker", "plot-uuid-999");
        var problemDetail = ErrorResponseAssembler.toProblemDetail(error);

        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("No se encontro el recurso ChillAccumulationTracker con id plot-uuid-999.", problemDetail.getDetail());
        assertEquals("https://api.viora.com/errors/chillaccumulationtracker-not-found", problemDetail.getType().toString());
    }

    @Test
    void toErrorResponseFromApplicationError_returnsResponseEntityWithProblemDetail() {
        var error = ApplicationError.validationError("plotName", "plot.name.blank");
        var response = ErrorResponseAssembler.toErrorResponseFromApplicationError(error);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
