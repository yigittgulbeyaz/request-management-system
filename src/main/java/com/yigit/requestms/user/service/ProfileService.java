package com.yigit.requestms.user.service;

import com.yigit.requestms.auth.service.PasswordPolicy;
import com.yigit.requestms.common.security.CurrentUserService;
import com.yigit.requestms.user.dto.PasswordChangeDto;
import com.yigit.requestms.user.dto.ProfileDto;
import com.yigit.requestms.user.dto.ProfileUpdateDto;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.exception.DuplicateEmailException;
import com.yigit.requestms.user.exception.IncorrectPasswordException;
import com.yigit.requestms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Everything here acts on the caller's own account, resolved from the session
// rather than taken as a parameter. No role restriction, because every role has
// an account and every account has an owner.
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository,
                          CurrentUserService currentUserService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileDto load() {
        UserEntity user = currentUserService.require();

        return new ProfileDto(
                user.getNameSurname(),
                user.getEmail(),
                user.getRole(),
                user.getSecurityQuestion());
    }

    // The role is not a parameter. Leaving it out is the whole protection: a
    // field that never arrives cannot be set by someone editing the request on
    // its way to the server.
    @Transactional
    public void update(ProfileUpdateDto form) {
        UserEntity user = currentUserService.require();
        String email = form.email().trim().toLowerCase();

        // Only a genuine change is checked, or saving a form without touching
        // the address would report a duplicate of yourself.
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        user.setNameSurname(form.nameSurname().trim());
        user.setEmail(email);
    }

    // Kept apart from update() rather than folded into one save. They ask for
    // different things and fail for different reasons, and a form that changes
    // a name and a password together has to explain which half went wrong.
    @Transactional
    public void changePassword(PasswordChangeDto form) {
        UserEntity user = currentUserService.require();

        if (!passwordEncoder.matches(form.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }

        PasswordPolicy.require(form.newPassword());
        user.setPasswordHash(passwordEncoder.encode(form.newPassword()));
    }
}