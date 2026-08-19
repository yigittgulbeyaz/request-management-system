package com.yigit.requestms.admin.service;

import com.yigit.requestms.admin.dto.AdminUserDto;
import com.yigit.requestms.admin.dto.CreateUserDto;
import com.yigit.requestms.admin.dto.CreatedUserDto;
import com.yigit.requestms.admin.dto.UserDetailDto;
import com.yigit.requestms.admin.exception.CannotDemoteLastAdminException;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.exception.DuplicateEmailException;
import com.yigit.requestms.user.exception.UserNotFoundException;
import com.yigit.requestms.user.repository.UserRepository;
import com.yigit.requestms.user.service.EmailPolicy;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // Long enough to hand over in person, short enough that a code left in a
    // notebook stops working before anyone finds it.
    private static final int SETUP_CODE_VALID_DAYS = 7;

    private final UserRepository userRepository;
    private final SetupCodeGenerator codeGenerator = new SetupCodeGenerator();

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> list(Role role, String search, Pageable pageable) {
        return userRepository.findForAdmin(role, blankToNull(search), pageable);
    }

    @Transactional(readOnly = true)
    public long count(Role role, String search) {
        return userRepository.countForAdmin(role, blankToNull(search));
    }

    @Transactional(readOnly = true)
    public UserDetailDto detail(Long userId) {
        UserEntity user = require(userId);

        return new UserDetailDto(
                user.getId(),
                user.getNameSurname(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isLocked(),
                user.isAwaitingSetup(),
                user.getFailedResetAttempts(),
                user.getSecurityQuestion(),
                user.getCreatedAt());
    }

    // The account is opened without credentials. What an administrator gets
    // back is a code to hand over, not a password to remember on someone
    // else's behalf: the person who will use the account chooses what guards
    // it, and the administrator's copy stops working the moment they do.
    //
    // The email is checked before the insert so the user reads a sentence
    // rather than a constraint violation, but the unique index is what
    // guarantees it: two administrators creating the same address at the same
    // moment would both pass this check.
    @Transactional
    public CreatedUserDto create(CreateUserDto form) {
        String email = form.email().trim().toLowerCase();

        // Checked here rather than trusted from the form. The form explains the
        // problem; this is what makes it a rule, because a caller reaching the
        // service directly never saw the form.
        EmailPolicy.require(email);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        String code = codeGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(SETUP_CODE_VALID_DAYS);

        UserEntity user = new UserEntity(
                form.nameSurname().trim(), email, form.role(), code, expiresAt);

        userRepository.save(user);

        return new CreatedUserDto(user.getId(), user.getEmail(), code, expiresAt);
    }

    // Issued again when the first code expires or goes astray, and for an
    // account that was locked out of its own recovery. Whatever the account had
    // is discarded: a reissued code is a fresh start, not a second key.
    @Transactional
    public CreatedUserDto reissueSetupCode(Long userId) {
        UserEntity user = require(userId);

        String code = codeGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(SETUP_CODE_VALID_DAYS);

        user.reissueSetupToken(code, expiresAt);

        return new CreatedUserDto(user.getId(), user.getEmail(), code, expiresAt);
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
    // Only the lock is cleared. The account keeps its password, because being
    // locked out of recovery says nothing about whether the owner still knows
    // how to sign in; someone who has forgotten needs a new setup code, which
    // is a separate decision.
    @Transactional
    public void unlock(Long userId) {
        UserEntity user = require(userId);
        user.setLocked(false);
        user.setFailedResetAttempts(0);
    }

    private UserEntity require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Counts the others rather than the total, so an administrator acting on a
    // colleague is allowed where one acting on themselves as the last
    // administrator is not.
    private void requireAnotherAdminRemains(UserEntity subject) {
        long activeAdmins = userRepository.countByRoleAndActiveTrue(Role.ADMIN);
        boolean subjectCounts = subject.isActive();

        if (activeAdmins - (subjectCounts ? 1 : 0) < 1) {
            throw new CannotDemoteLastAdminException();
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}