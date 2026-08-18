package com.yigit.requestms.auth.dto;

import com.yigit.requestms.user.enums.SecurityQuestion;

// Everything the account was missing, supplied at once by the person who will
// use it. The code identifies the account; the rest is what will guard it from
// here on.
public record AccountSetupDto(
        String setupCode,
        String password,
        SecurityQuestion securityQuestion,
        String securityAnswer
) {
}