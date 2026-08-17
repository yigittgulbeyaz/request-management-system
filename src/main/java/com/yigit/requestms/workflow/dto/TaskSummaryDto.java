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
        LocalDateTime createdAt,
        LocalDateTime deadline
) {

    // Null on tasks that predate the rule, and never overdue once finished:
    // a task delivered late is late, but it is not still running out of time.
    public boolean isOverdue() {
        return deadline != null
                && status != WorkflowStatus.DONE
                && LocalDateTime.now().isAfter(deadline);
    }

    public boolean isDueSoon() {
        return deadline != null
                && status != WorkflowStatus.DONE
                && !isOverdue()
                && LocalDateTime.now().plusDays(2).isAfter(deadline);
    }
}