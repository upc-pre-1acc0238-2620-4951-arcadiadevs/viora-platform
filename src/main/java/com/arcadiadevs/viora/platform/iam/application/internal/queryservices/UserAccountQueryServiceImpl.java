package com.arcadiadevs.viora.platform.iam.application.internal.queryservices;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.application.queryservices.UserAccountQueryService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckPasswordResetTokenQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckUserAccountExistsByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByIdQuery;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Read-only application service implementation for querying user accounts.
 */
@Service
@Transactional(readOnly = true)
public class UserAccountQueryServiceImpl implements UserAccountQueryService {

    private final UserAccountRepository userAccountRepository;
    private final SecureTokenGenerator tokenGenerator;

    /**
     * Creates the query service.
     *
     * @param userAccountRepository the user account repository port
     * @param tokenGenerator        hashes the token of a reset link to look it up
     * @throws IllegalArgumentException if any dependency is null
     */
    public UserAccountQueryServiceImpl(UserAccountRepository userAccountRepository, SecureTokenGenerator tokenGenerator) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        if (tokenGenerator == null) {
            throw new IllegalArgumentException("user.token_generator.null");
        }
        this.userAccountRepository = userAccountRepository;
        this.tokenGenerator = tokenGenerator;
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

    @Override
    public boolean handle(CheckPasswordResetTokenQuery query) {
        if (query == null || query.resetToken().isBlank()) {
            return false;
        }
        var tokenHash = tokenGenerator.hash(query.resetToken().trim());
        return userAccountRepository.findByResetTokenHash(tokenHash)
                .map(account -> account.canResetPasswordWith(tokenHash, Instant.now()))
                .orElse(false);
    }
}
