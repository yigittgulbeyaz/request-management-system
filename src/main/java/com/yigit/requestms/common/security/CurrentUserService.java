package com.yigit.requestms.common.security;

import com.yigit.requestms.common.exception.UnauthenticatedException;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.repository.UserRepository;
import com.yigit.requestms.user.service.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// Single point where the acting user is resolved. Services never take a user id
// as a parameter, so a client cannot act on someone else's behalf.
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new UnauthenticatedException("No authenticated user in context");
        }
        return details.getUserId();
    }

    // Reaching here with a missing row means the account was deleted mid-session,
    // which soft delete is supposed to make impossible.
    public UserEntity require() {
        Long id = requireId();
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthenticatedException(
                        "Authenticated user no longer exists: " + id));
    }
}