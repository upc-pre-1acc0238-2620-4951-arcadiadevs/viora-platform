package com.arcadiadevs.viora.platform.iam.domain.model.queries;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;

/**
 * Domain query record for verifying whether a user account exists with the given email address.
 *
 * @param email the email address VO
 */
public record CheckUserAccountExistsByEmailQuery(EmailAddress email) {

    public CheckUserAccountExistsByEmailQuery {
        if (email == null) {
            throw new IllegalArgumentException("user.email.blank");
        }
    }

    public CheckUserAccountExistsByEmailQuery(String rawEmail) {
        this(new EmailAddress(rawEmail));
    }
}
