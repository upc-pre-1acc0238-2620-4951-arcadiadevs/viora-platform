package com.arcadiadevs.viora.platform.iam.domain.model.queries;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;

/**
 * Domain query record for resolving a user account by its normalized email address.
 *
 * @param email the email address VO
 */
public record GetUserAccountByEmailQuery(EmailAddress email) {

    public GetUserAccountByEmailQuery {
        if (email == null) {
            throw new IllegalArgumentException("user.email.blank");
        }
    }

    public GetUserAccountByEmailQuery(String rawEmail) {
        this(new EmailAddress(rawEmail));
    }
}
