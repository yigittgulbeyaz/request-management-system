package com.yigit.requestms.admin.dto;

import com.yigit.requestms.user.enums.Role;
import com.yigit.requestms.user.enums.SecurityQuestion;

import java.time.LocalDateTime;

// What an administrator sees when they open one account. Wider than the list
// row because a detail view answers different questions: not "who is in the
// system" but "who is this, and what have they been doing".
//
// The statistics that answer the second half arrive with the reporting views;
// this carries the account itself.
public record UserDetailDto(
        Long id,
        String nameSurname,
        String email,
        Role role,
        boolean active,
        boolean locked,
        boolean awaitingSetup,
        int failedResetAttempts,
        SecurityQuestion securityQuestion,
        LocalDateTime createdAt
) {
}