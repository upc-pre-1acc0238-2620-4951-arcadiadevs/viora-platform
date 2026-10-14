package com.arcadiadevs.viora.platform.iam.application.internal.queryservices;

import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckUserAccountExistsByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByIdQuery;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Read-only application service implementation for querying user accounts.
 */
@Service
@Transactional(readOnly = true)
public class UserAccountQueryServiceImpl implements UserAccountQueryService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountQueryServiceImpl(UserAccountRepository userAccountRepository) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public Optional<UserAccount> handle(GetUserAccountByIdQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        return userAccountRepository.findById(query.userId());
    }

    @Override
    public Optional<UserAccount> handle(GetUserAccountByEmailQuery query) {
        if (query == null) {
            return Optional.empty();
        }
        return userAccountRepository.findByEmail(query.email());
    }

    @Override
    public boolean handle(CheckUserAccountExistsByEmailQuery query) {
        if (query == null) {
            return false;
        }
        return userAccountRepository.existsByEmail(query.email());
    }
}
