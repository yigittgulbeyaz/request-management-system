package com.yigit.requestms.admin.dto;

import com.yigit.requestms.user.enums.Role;

import java.time.LocalDateTime;

// One row of the user list. Carries no password material of any kind: an
// administrator manages accounts, and nothing about managing them needs the
// hashes or the setup code.
public record AdminUserDto(
        Long id,
        String nameSurname,
        String email,
        Role role,
        boolean active,
        boolean locked,
        boolean awaitingSetup,
        LocalDateTime createdAt
) {
}