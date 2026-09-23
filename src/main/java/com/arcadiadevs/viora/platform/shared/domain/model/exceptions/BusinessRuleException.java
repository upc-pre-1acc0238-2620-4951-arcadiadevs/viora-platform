package com.arcadiadevs.viora.platform.shared.domain.model.exceptions;

/**
 * Exception thrown when a domain business rule or constraint is violated.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Constructs a BusinessRuleException with the specified detail message.
     *
     * @param message the detail message
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    /**
     * Constructs a BusinessRuleException for a given rule and reason.
     *
     * @param rule   the violated business rule description
     * @param reason the detailed reason for the violation
     */
    public BusinessRuleException(String rule, String reason) {
        super("Business rule '%s' violated: %s".formatted(rule, reason));
    }
}
