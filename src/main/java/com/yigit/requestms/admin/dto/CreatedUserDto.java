package com.yigit.requestms.admin.dto;

// Returned once, straight after creation, and never stored anywhere it could
// be read again. The temporary password exists to be handed over and replaced.
public record CreatedUserDto(
        Long userId,
        String email,
        String temporaryPassword
) {
}