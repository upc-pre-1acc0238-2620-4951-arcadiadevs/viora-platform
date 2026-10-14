package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.services;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service loading user account details for Spring Security authentication.
 */
@Service(value = "defaultUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public UserDetailsServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("user.username.blank");
        }

        Optional<UserAccount> userAccount;
        if (username.contains("@")) {
            userAccount = userAccountRepository.findByEmail(new EmailAddress(username));
        } else {
            userAccount = userAccountRepository.findById(new UserId(username));
        }

        return userAccount
                .map(UserDetailsImpl::build)
                .orElseThrow(() -> new UsernameNotFoundException("user.not_found"));
    }
}
