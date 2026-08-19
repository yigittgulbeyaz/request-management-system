package com.yigit.requestms.notification.dto;

import java.time.LocalDateTime;

// One line in the bell menu. Carries the request id so the notice can be
// clicked through to whatever it is about, and null when it refers to nothing
// in particular.
public record NotificationDto(
        Long id,
        String message,
        boolean read,
        Long relatedRequestId,
        LocalDateTime createdAt
) {
}