package com.yigit.requestms.auth.dto;

// What the recovery form sends back once the question has been answered. The
// email identifies the account, the answer proves it belongs to whoever is
// asking, and the password replaces what they forgot.
public record PasswordRecoveryDto(
        String email,
        String securityAnswer,
        String newPassword
) {
}