package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/**
 * Immutable server-calculated payment terms.
 * @param base net annual amount
 * @param tax tax amount
 * @param total payable amount
 */
public record SubscriptionPrice(Money base, Money tax, Money total) {
    /**
     * Requires consistent monetary terms.
     * @throws IllegalArgumentException if an amount is missing or the sum differs
     */
    public SubscriptionPrice {
        if (base == null || tax == null || total == null
                || base.amount().add(tax.amount()).compareTo(total.amount()) != 0) {
            throw new IllegalArgumentException("subscription.price.invalid");
        }
    }
}
