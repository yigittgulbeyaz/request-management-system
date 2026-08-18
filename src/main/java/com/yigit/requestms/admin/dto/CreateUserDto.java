package com.yigit.requestms.admin.dto;

import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;

// What an administrator supplies to open an account. No password: one is
// generated and shown once, and the account is flagged to demand a new one at
// first sign-in, so an administrator never chooses or learns a lasting
// password for someone else.
public record CreateUserDto(
        String nameSurname,
        String email,
        Role role,
        SecurityQuestion securityQuestion,
        String securityAnswer
) {
}