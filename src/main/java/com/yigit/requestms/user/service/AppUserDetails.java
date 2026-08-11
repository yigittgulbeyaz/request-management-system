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
    private final boolean mustChangePassword;
    private final List<GrantedAuthority> authorities;

    public AppUserDetails(UserEntity user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
        this.locked = user.isLocked();
        this.mustChangePassword = user.isMustChangePassword();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().asAuthority()));
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

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

    @Override
    public boolean isEnabled() {
        return active;
    }
}