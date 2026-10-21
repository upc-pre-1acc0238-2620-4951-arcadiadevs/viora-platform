package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

/** Immutable terms authorized by the server. @param base net annual amount @param tax tax amount @param total payable amount */
public record SubscriptionPrice(Money base, Money tax, Money total) {
    public SubscriptionPrice {
        if (base == null || tax == null || total == null
                || base.amount().add(tax.amount()).compareTo(total.amount()) != 0) {
            throw new IllegalArgumentException("subscription.price.invalid");
        }
    }
}
