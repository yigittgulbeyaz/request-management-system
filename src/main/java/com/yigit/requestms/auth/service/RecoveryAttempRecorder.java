package com.yigit.requestms.auth.service;

import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// A class of its own because REQUIRES_NEW has to cross a proxy to take effect.
// Called from inside PasswordRecoveryService it would be a plain method call,
// Spring would never see it, and the new transaction would silently be the old
// one.
//
// The separation is what makes the count survive: the caller is about to throw,
// its transaction is about to roll back, and an attempt that rolled back is an
// attempt that never happened.
@Service
class RecoveryAttemptRecorder {

    // Three is enough for someone who knows the answer and mistypes it, and
    // few enough that guessing between four fixed questions is not worth
    // starting.
    private static final int MAX_ATTEMPTS = 3;

    private final UserRepository userRepository;

    RecoveryAttemptRecorder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Reloads the account rather than taking the caller's copy: that one
    // belongs to a transaction on its way out, and writing through it would put
    // the change back where it is about to be discarded.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordFailure(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedResetAttempts() + 1;
            user.setFailedResetAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setLocked(true);
            }
        });
    }
}