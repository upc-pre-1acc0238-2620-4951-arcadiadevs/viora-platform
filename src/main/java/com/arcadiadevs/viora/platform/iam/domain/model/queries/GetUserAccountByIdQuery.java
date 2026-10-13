package com.arcadiadevs.viora.platform.iam.domain.model.queries;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;

/**
 * Domain query record for resolving a user account by its primary identifier.
 *
 * @param userId the user account identifier VO
 */
public record GetUserAccountByIdQuery(UserId userId) {

    public GetUserAccountByIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
    }

    public GetUserAccountByIdQuery(String rawUserId) {
        this(new UserId(rawUserId));
    }
}
