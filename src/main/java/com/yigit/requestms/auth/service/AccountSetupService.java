package com.yigit.requestms.auth.service;

import com.yigit.requestms.auth.dto.AccountSetupDto;
import com.yigit.requestms.auth.exception.InvalidSetupCodeException;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reachable without signing in, because the person using it has no account to
// sign in with yet. The code is the only credential, which is why it is
// single-use and why every way of failing looks the same from outside.
@Service
public class AccountSetupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountSetupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Asked before showing the form, so someone with a bad code is turned away
    // at the door rather than after filling in a password.
    @Transactional(readOnly = true)
    public boolean isUsable(String setupCode) {
        return userRepository.findBySetupToken(normalise(setupCode))
                .filter(UserEntity::isActive)
                .filter(user -> !user.isSetupTokenExpired())
                .isPresent();
    }

    @Transactional(readOnly = true)
    public String nameFor(String setupCode) {
        return userRepository.findBySetupToken(normalise(setupCode))
                .map(UserEntity::getNameSurname)
                .orElseThrow(InvalidSetupCodeException::new);
    }

    // The password, the question and the answer land together, and the code
    // that allowed them is destroyed in the same transaction: there is no
    // moment where both the code and the password open the account.
    @Transactional
    public void complete(AccountSetupDto form) {
        UserEntity user = userRepository.findBySetupToken(normalise(form.setupCode()))
                .filter(UserEntity::isActive)
                .filter(candidate -> !candidate.isSetupTokenExpired())
                .orElseThrow(InvalidSetupCodeException::new);

        PasswordPolicy.require(form.password());

        user.completeSetup(
                passwordEncoder.encode(form.password()),
                form.securityQuestion(),
                passwordEncoder.encode(normaliseAnswer(form.securityAnswer())));
    }

    // Codes are shown in upper case and grouped with dashes, so anyone typing
    // one back may well use lower case or leave the dashes out.
    private String normalise(String setupCode) {
        return setupCode == null ? "" : setupCode.trim().toUpperCase();
    }

    // The answer is compared after the same treatment, so a capital letter or a
    // stray space cannot lock someone out of their own account.
    private String normaliseAnswer(String answer) {
        return answer.trim().toLowerCase();
    }
}