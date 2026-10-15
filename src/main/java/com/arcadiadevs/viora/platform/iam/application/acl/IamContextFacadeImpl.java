package com.arcadiadevs.viora.platform.iam.application.acl;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of {@link IamContextFacade} translating inter-context queries to domain lookups.
 */
@Service
@Transactional(readOnly = true)
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserAccountRepository userAccountRepository;

    public IamContextFacadeImpl(UserAccountRepository userAccountRepository) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public boolean existsActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            return userAccountRepository.findById(new UserId(userId)).isPresent();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public Optional<String> findUserRole(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return userAccountRepository.findById(new UserId(userId))
                    .map(account -> account.snapshot().role().name());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findUserEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return userAccountRepository.findById(new UserId(userId))
                    .map(account -> account.snapshot().email().value());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
