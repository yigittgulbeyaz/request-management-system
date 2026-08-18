package com.yigit.requestms.admin.service;

import com.yigit.requestms.admin.dto.AdminUserDto;
import com.yigit.requestms.admin.dto.CreateUserDto;
import com.yigit.requestms.admin.dto.CreatedUserDto;
import com.yigit.requestms.admin.exception.CannotDemoteLastAdminException;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.exception.DuplicateEmailException;
import com.yigit.requestms.user.exception.UserNotFoundException;
import com.yigit.requestms.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Account administration, kept apart from UserService: that one answers a
// user's questions about themselves, this one acts on anyone. Two different
// authorisation rules do not belong behind one class.
//
// Nothing here touches requests, scores or tasks. An administrator manages who
// may use the system, not what the system decides.
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator passwordGenerator = new TemporaryPasswordGenerator();

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> list(Role role, String search, Pageable pageable) {
        return userRepository.findForAdmin(role, blankToNull(search), pageable);
    }

    @Transactional(readOnly = true)
    public long count(Role role, String search) {
        return userRepository.countForAdmin(role, blankToNull(search));
    }

    // The email is checked before the insert so the user reads a sentence
    // rather than a constraint violation, but the unique index is what actually
    // guarantees it: two administrators creating the same address at the same
    // moment would both pass this check.
    @Transactional
    public CreatedUserDto create(CreateUserDto form) {
        String email = form.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        String temporaryPassword = passwordGenerator.generate();

        UserEntity user = new UserEntity(
                form.nameSurname().trim(),
                email,
                passwordEncoder.encode(temporaryPassword),
                form.role(),
                form.securityQuestion(),
                passwordEncoder.encode(normaliseAnswer(form.securityAnswer())));

        user.setMustChangePassword(true);
        userRepository.save(user);

        // Returned once and stored nowhere it can be read again. The hash is
        // all that persists.
        return new CreatedUserDto(user.getId(), user.getEmail(), temporaryPassword);
    }

    @Transactional
    public void changeRole(Long userId, Role newRole) {
        UserEntity user = require(userId);

        if (user.getRole() == newRole) {
            return;
        }
        if (user.getRole() == Role.ADMIN) {
            requireAnotherAdminRemains(user);
        }

        // Past work keeps the author it had. The role says what someone may do
        // from now on, not what they were doing when they did it.
        user.setRole(newRole);
    }

    // Deactivation rather than deletion. Removing the row would break the
    // foreign keys in workflows, prioritizations and status history, which is
    // to say it would erase the work of whoever left.
    @Transactional
    public void deactivate(Long userId) {
        UserEntity user = require(userId);

        if (user.getRole() == Role.ADMIN) {
            requireAnotherAdminRemains(user);
        }
        user.setActive(false);
    }

    @Transactional
    public void reactivate(Long userId) {
        require(userId).setActive(true);
    }

    // Locked and inactive are different states with different causes: locked is
    // what the system does after too many failed reset attempts, inactive is
    // what an administrator decided. Unlocking does not reactivate, and
    // reactivating does not unlock.
    //
    // The password is replaced along with the unlock because the account was
    // locked by someone failing to prove they owned it, and the old password
    // may be the reason they were trying.
    @Transactional
    public CreatedUserDto unlockWithNewPassword(Long userId) {
        UserEntity user = require(userId);
        String temporaryPassword = passwordGenerator.generate();

        user.setLocked(false);
        user.setFailedResetAttempts(0);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);

        return new CreatedUserDto(user.getId(), user.getEmail(), temporaryPassword);
    }

    private UserEntity require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Counts the others rather than the total, so an administrator acting on
    // their own account is refused while one acting on a colleague is not.
    private void requireAnotherAdminRemains(UserEntity subject) {
        long activeAdmins = userRepository.countByRoleAndActiveTrue(Role.ADMIN);
        boolean subjectCounts = subject.isActive();

        if (activeAdmins - (subjectCounts ? 1 : 0) < 1) {
            throw new CannotDemoteLastAdminException();
        }
    }

    // The answer is compared after the same treatment it gets here, so a
    // capital letter or a stray space at either end cannot lock someone out of
    // their own account.
    private String normaliseAnswer(String answer) {
        return answer.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}