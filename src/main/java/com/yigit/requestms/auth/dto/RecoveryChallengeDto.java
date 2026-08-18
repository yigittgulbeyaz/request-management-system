package com.yigit.requestms.auth.dto;

import com.yigit.requestms.user.enums.SecurityQuestion;

// The question put to whoever typed an email address. It is answered the same
// way whether the account exists or not: an unknown address gets a plausible
// question rather than a denial, so the form cannot be used to find out which
// addresses are real.
public record RecoveryChallengeDto(
        SecurityQuestion question,
        boolean accountExists
) {
}