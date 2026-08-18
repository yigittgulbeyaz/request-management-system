package com.yigit.requestms.user.dto;

import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;

// What someone sees about their own account. The role is here to be read, not
// changed: knowing what you are is useful, and deciding it is an
// administrator's job.
public record ProfileDto(
        String nameSurname,
        String email,
        Role role,
        SecurityQuestion securityQuestion
) {
}