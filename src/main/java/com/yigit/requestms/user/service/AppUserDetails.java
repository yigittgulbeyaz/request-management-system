package com.yigit.requestms.user.service;

import com.yigit.requestms.user.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Carries the user id alongside the credentials so the acting user can be
// resolved from the session without a second lookup on every request.
public class AppUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final boolean locked;
    private final boolean awaitingSetup;
    private final List<GrantedAuthority> authorities;

    public AppUserDetails(UserEntity user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
        this.locked = user.isLocked();
        this.awaitingSetup = user.isAwaitingSetup();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().asAuthority()));
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // Null while an account is waiting to be set up. Spring never compares
    // against it because isEnabled already refused, but the field is honest
    // about what the account has.
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    // An account waiting for setup has no password to check against. Reporting
    // it as disabled puts the refusal where Spring already looks, rather than
    // leaving a null hash to fail somewhere less predictable.
    @Override
    public boolean isEnabled() {
        return active && !awaitingSetup;
    }
}