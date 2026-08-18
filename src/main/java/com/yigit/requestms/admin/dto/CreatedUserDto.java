package com.yigit.requestms.admin.dto;

import java.time.LocalDateTime;

// Returned once, straight after the account is opened. The code is what an
// administrator hands over; nothing stores it in a form anyone can read back,
// and using it destroys it.
public record CreatedUserDto(
        Long userId,
        String email,
        String setupCode,
        LocalDateTime expiresAt
) {
}