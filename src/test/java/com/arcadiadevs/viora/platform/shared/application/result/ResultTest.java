package com.arcadiadevs.viora.platform.shared.application.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void exposesSuccessfulValue() {
        Result<String, ApplicationError> result = Result.success("ok");

        assertTrue(result.isSuccess());
        assertEquals("ok", result.success().orElseThrow());
    }

    @Test
    void exposesFailureValue() {
        var error = ApplicationError.notFound("item", "1");
        Result<String, ApplicationError> result = Result.failure(error);

        assertTrue(result.isFailure());
        assertEquals(error, result.failure().orElseThrow());
    }

    @Test
    void foldsBothResultBranches() {
        Result<Integer, String> success = Result.success(4);
        Result<Integer, String> failure = Result.failure("failed");

        assertEquals("value:4", success.fold(
                value -> "value:" + value,
                error -> "error:" + error
        ));
        assertEquals("error:failed", failure.fold(
                value -> "value:" + value,
                error -> "error:" + error
        ));
    }

    @Test
    void mapsSuccessfulValue() {
        Result<Integer, String> result = Result.success(5);
        Result<String, String> mapped = result.map(i -> "num:" + i);

        assertTrue(mapped.isSuccess());
        assertEquals("num:5", mapped.success().orElseThrow());
    }

    @Test
    void mapsErrorOnFailure() {
        Result<Integer, String> result = Result.failure("bad");
        Result<Integer, Integer> mapped = result.mapError(String::length);

        assertTrue(mapped.isFailure());
        assertEquals(3, mapped.failure().orElseThrow());
    }

    @Test
    void flatMapsSuccessToNewResult() {
        Result<Integer, String> result = Result.success(10);
        Result<Integer, String> flatMapped = result.flatMap(i -> Result.success(i * 2));

        assertTrue(flatMapped.isSuccess());
        assertEquals(20, flatMapped.success().orElseThrow());
    }

    @Test
    void getsOrElseReturnsFallbackOnFailure() {
        Result<String, String> failure = Result.failure("error");
        assertEquals("fallback", failure.getOrElse("fallback"));

        Result<String, String> success = Result.success("actual");
        assertEquals("actual", success.getOrElse("fallback"));
    }

    @Test
    void recoversFromFailureWithFallbackResult() {
        Result<String, String> failure = Result.failure("error");
        Result<String, String> recovered = failure.recover(err -> Result.success("recovered-" + err));

        assertTrue(recovered.isSuccess());
        assertEquals("recovered-error", recovered.success().orElseThrow());
    }

    @Test
    void toOptionalConvertsCorrectly() {
        Result<String, String> success = Result.success("value");
        Result<String, String> failure = Result.failure("err");

        assertTrue(success.toOptional().isPresent());
        assertEquals("value", success.toOptional().orElseThrow());
        assertFalse(failure.toOptional().isPresent());
    }
}
