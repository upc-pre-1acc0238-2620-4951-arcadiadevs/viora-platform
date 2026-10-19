package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.model;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter providing user credentials and granted authorities to Spring Security.
 */
@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

    private final String username;
    @JsonIgnore
    private final String password;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    /** Credentials version of the account: a bearer token issued with another version is rejected. */
    private final long credentialsVersion;

    public UserDetailsImpl(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this(username, password, authorities, 0L);
    }

    public UserDetailsImpl(String username, String password, Collection<? extends GrantedAuthority> authorities,
                           long credentialsVersion) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.credentialsVersion = credentialsVersion;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    /**
     * Builds a {@link UserDetailsImpl} adapter from a pure {@link UserAccount} domain aggregate.
     *
     * @param userAccount domain aggregate root
     * @return UserDetailsImpl instance
     */
    public static UserDetailsImpl build(UserAccount userAccount) {
        var snapshot = userAccount.snapshot();
        var authorities = List.of(new SimpleGrantedAuthority(snapshot.role().name()));
        return new UserDetailsImpl(
                snapshot.id().userId(),
                snapshot.passwordHash().value(),
                authorities,
                snapshot.credentialsVersion()
        );
    }
}
