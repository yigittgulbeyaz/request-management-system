package com.yigit.requestms.user.dto;

// The current password is required even though the session already proves who
// is asking. A session left open on a shared machine proves the machine, not
// the person, and this is the one place that difference matters.
public record PasswordChangeDto(
        String currentPassword,
        String newPassword
) {
}