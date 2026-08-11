package com.yigit.requestms.request.dto;

import com.yigit.requestms.request.enums.RequestStatus;

import java.time.LocalDateTime;

// What a customer is allowed to see about their own request. Impact, urgency
// and priority score are absent by design, not nulled out: the score is the
// product owner's planning tool, and a DTO that never carries it cannot leak it.
//
// Description is absent too, but for a different reason: it is a CLOB fetched
// separately, and the grid does not display it.
public record CustomerRequestDto(
        Long id,
        String title,
        RequestStatus status,
        String rejectionReason,
        LocalDateTime createdAt
) {
}