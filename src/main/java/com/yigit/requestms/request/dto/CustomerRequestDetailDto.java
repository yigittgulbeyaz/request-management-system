package com.yigit.requestms.request.dto;

import com.yigit.requestms.request.enums.RequestStatus;

import java.time.LocalDateTime;

// Loaded only when the customer opens a single request, which is the one place
// the CLOB is actually shown.
public record CustomerRequestDetailDto(
        Long id,
        String title,
        String description,
        RequestStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}