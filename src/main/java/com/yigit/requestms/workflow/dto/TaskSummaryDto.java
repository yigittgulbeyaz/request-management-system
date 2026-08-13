package com.yigit.requestms.workflow.dto;

import com.yigit.requestms.workflow.enums.WorkflowStatus;

import java.time.LocalDateTime;

// One row of a developer's board. The priority score is here because it is
// what tells the developer which task to pull next; the customer's name is
// not, because who asked does not change what needs building.
public record TaskSummaryDto(
        Long taskId,
        Long requestId,
        String requestTitle,
        Integer priorityScore,
        WorkflowStatus status,
        String developerName,
        LocalDateTime createdAt
) {
}