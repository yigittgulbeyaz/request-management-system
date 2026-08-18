package com.yigit.requestms.auth.service;

import com.yigit.requestms.auth.dto.PasswordRecoveryDto;
import com.yigit.requestms.auth.dto.RecoveryChallengeDto;
import com.yigit.requestms.auth.exception.AccountLockedException;
import com.yigit.requestms.auth.exception.InvalidSecurityAnswerException;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.SecurityQuestion;
import com.yigit.requestms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Reachable without signing in, which is the point: the person using it cannot
// sign in. That makes it the most exposed surface in the system, so it is
// careful about what it reveals and how many tries it allows.
@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryAttemptRecorder attemptRecorder;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   RecoveryAttemptRecorder attemptRecorder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.attemptRecorder = attemptRecorder;
    }

    // An unknown address gets a question rather than a refusal. Saying "no such
    // account" would turn this form into a way of testing which addresses are
    // registered, which is worth more to someone probing than to someone who
    // mistyped their own.
    //
    // The question is derived from the address, so the same unknown address
    // always gets the same one: a question that changed between attempts would
    // give the pretence away.
    @Transactional(readOnly = true)
    public RecoveryChallengeDto challengeFor(String email) {
        Optional<UserEntity> account = userRepository.findByEmail(normalise(email))
                .filter(UserEntity::isActive)
                .filter(user -> !user.isAwaitingSetup());

        return account
                .map(user -> new RecoveryChallengeDto(user.getSecurityQuestion(), true))
                .orElseGet(() -> new RecoveryChallengeDto(plausibleQuestionFor(email), false));
    }

    // An unknown address and a wrong answer fail identically, so the form
    // cannot be used to tell them apart.
    //
    // The password rule is checked first: someone who answered correctly should
    // not lose the attempt because they also chose a short password.
    @Transactional
    public void recover(PasswordRecoveryDto form) {
        PasswordPolicy.require(form.newPassword());

        UserEntity user = userRepository.findByEmail(normalise(form.email()))
                .filter(UserEntity::isActive)
                .filter(candidate -> !candidate.isAwaitingSetup())
                .orElseThrow(InvalidSecurityAnswerException::new);

        if (user.isLocked()) {
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(normaliseAnswer(form.securityAnswer()),
                user.getSecurityAnswerHash())) {
            // Recorded in a transaction of its own, because this one is about
            // to be rolled back by the exception on the next line.
            attemptRecorder.recordFailure(user.getId());
            throw new InvalidSecurityAnswerException();
        }

        user.setPasswordHash(passwordEncoder.encode(form.newPassword()));
        user.setFailedResetAttempts(0);
    }

    // Deterministic from the address, so an unknown one is asked the same thing
    // every time. Nothing about which question matters except that it stays put.
    private SecurityQuestion plausibleQuestionFor(String email) {
        SecurityQuestion[] questions = SecurityQuestion.values();
        int index = Math.floorMod(normalise(email).hashCode(), questions.length);
        return questions[index];
    }

    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    // Compared after the same treatment it was stored with, so a capital letter
    // or a stray space cannot lock someone out of their own account.
    private String normaliseAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase();
    }
}