package com.yigit.requestms.request.dto;

import java.time.LocalDateTime;

// One step in what happened to a request. The status is carried as the stored
// string rather than an enum, because the trail records both machines and only
// some of those values belong to RequestStatus.
public record StatusTimelineEntryDto(
        String newStatus,
        LocalDateTime changedAt
) {
}