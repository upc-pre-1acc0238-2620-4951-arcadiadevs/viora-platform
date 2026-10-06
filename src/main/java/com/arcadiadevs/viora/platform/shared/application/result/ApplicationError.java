package com.arcadiadevs.viora.platform.shared.application.result;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Standard error representation used within the application layer.
 * Encapsulates an error code, a descriptive message, and optional context details.
 *
 * <p>An error may also carry extra machine readable properties, which the REST layer copies onto the RFC 7807
 * problem detail. They let a failure answer what the client needs to recover, for example the settlement that is
 * already registered when a campaign is settled twice, without inventing a new error code for every situation.</p>
 *
 * @param code       the unique machine-readable error code
 * @param message    human-readable error description
 * @param details    optional detailed context or reasons for the failure
 * @param properties extra machine-readable context copied onto the problem detail; empty by default
 */
@NullMarked
public record ApplicationError(
        String code,
        String message,
        @Nullable String details,
        Map<String, Object> properties) {

    /**
     * Normalizes the properties so callers never have to pass an empty map explicitly.
     */
    public ApplicationError {
        properties = properties == null ? Collections.emptyMap() : Map.copyOf(properties);
    }

    /**
     * Creates an ApplicationError with code, message and details, and no extra properties.
     *
     * @param code    the unique error code
     * @param message human-readable error description
     * @param details optional detailed context or reasons for the failure
     */
    public ApplicationError(String code, String message, @Nullable String details) {
        this(code, message, details, Collections.emptyMap());
    }

    /**
     * Creates an ApplicationError with code and message only.
     *
     * @param code    the unique error code
     * @param message human-readable error description
     */
    public ApplicationError(String code, String message) {
        this(code, message, null, Collections.emptyMap());
    }

    /**
     * Creates a validation error when input data is invalid or violates constraints.
     *
     * @param fieldOrConcept the invalid field or concept name
     * @param reason         the reason why validation failed
     * @return an {@link ApplicationError} configured for validation failure
     */
    public static ApplicationError validationError(String fieldOrConcept, String reason) {
        return new ApplicationError(
                "VALIDATION_ERROR",
                "Validation failed: %s".formatted(fieldOrConcept),
                reason);
    }

    /**
     * Creates a not-found error when the requested resource does not exist.
     *
     * @param resourceType the type of the resource (e.g., "item", "resource")
     * @param identifier   the identifier used in the lookup
     * @return an {@link ApplicationError} configured for not-found scenarios
     */
    public static ApplicationError notFound(String resourceType, String identifier) {
        return new ApplicationError(
                "%s_NOT_FOUND".formatted(resourceType.toUpperCase()),
                "%s not found: %s".formatted(resourceType, identifier),
                identifier);
    }

    /**
     * Creates a business rule violation error when an operation violates domain constraints.
     *
     * @param rule   the violated business rule description
     * @param reason detailed reason or context for the violation
     * @return an {@link ApplicationError} configured for business rule violations
     */
    public static ApplicationError businessRuleViolation(String rule, String reason) {
        return new ApplicationError(
                "BUSINESS_RULE_VIOLATION",
                "Business rule violation: %s".formatted(rule),
                reason);
    }

    /**
     * Creates a conflict error when an operation cannot be completed due to conflicting state.
     *
     * @param resource the name of the conflicting resource
     * @param reason   the explanation of the conflict
     * @return an {@link ApplicationError} configured for conflict conditions
     */
    public static ApplicationError conflict(String resource, String reason) {
        return new ApplicationError(
                "%s_CONFLICT".formatted(resource.toUpperCase()),
                "Conflict with %s".formatted(resource),
                reason);
    }

    /**
     * Creates a conflict error that also tells the client what already exists, so it can recover without a second
     * call.
     *
     * @param resource   the name of the conflicting resource
     * @param reason     the explanation of the conflict
     * @param properties extra machine-readable context copied onto the problem detail, such as the record that is
     *                   already registered
     * @return an {@link ApplicationError} configured for conflict conditions, with the extra context
     */
    public static ApplicationError conflict(String resource, String reason, Map<String, Object> properties) {
        return new ApplicationError(
                "%s_CONFLICT".formatted(resource.toUpperCase()),
                "Conflict with %s".formatted(resource),
                reason,
                properties);
    }

    /**
     * Creates a forbidden error when the authenticated actor cannot access the resource.
     *
     * @param resource the resource being accessed
     * @param reason   the reason for denying access
     * @return an {@link ApplicationError} configured for forbidden access
     */
    public static ApplicationError forbidden(String resource, String reason) {
        return new ApplicationError(
                "%s_FORBIDDEN".formatted(resource.toUpperCase().replace('-', '_')),
                "Access forbidden: %s".formatted(resource),
                reason);
    }

    /**
     * Creates a precondition failed error when optimistic locking or headers (e.g. If-Match) fail.
     *
     * @param resource the resource being mutated
     * @param reason   the reason why the precondition failed
     * @return an {@link ApplicationError} configured for precondition failures
     */
    public static ApplicationError preconditionFailed(String resource, String reason) {
        return new ApplicationError(
                "%s_PRECONDITION_FAILED".formatted(resource.toUpperCase().replace('-', '_')),
                "Precondition failed for %s".formatted(resource),
                reason);
    }

    /**
     * Creates an unexpected error when an unanticipated exception or state occurs.
     *
     * @param context the context or component where the error occurred
     * @param reason  the detailed error explanation
     * @return an {@link ApplicationError} configured for unexpected failures
     */
    public static ApplicationError unexpected(String context, String reason) {
        return new ApplicationError(
                "UNEXPECTED_ERROR",
                "Unexpected error in %s".formatted(context),
                reason);
    }

}
