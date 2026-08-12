package com.yigit.requestms.request.dto;

import com.yigit.requestms.request.enums.RequestStatus;

import java.time.LocalDateTime;

// One row of the prioritization pool, spanning three tables. Built by a
// constructor expression rather than by walking entity associations, which
// would issue a query per row for the customer and another for the score.
//
// priorityScore is null when the request has not been scored yet: the absence
// of a prioritization row is what "Not Assigned" means, so no separate flag
// is needed.
public record RequestSummaryDto(
        Long id,
        String customerName,
        String title,
        Integer priorityScore,
        RequestStatus status,
        LocalDateTime createdAt
) {
}