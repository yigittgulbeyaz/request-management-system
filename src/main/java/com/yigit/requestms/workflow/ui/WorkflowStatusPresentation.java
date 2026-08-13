package com.yigit.requestms.workflow.ui;

import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.workflow.enums.WorkflowStatus;

// How a stage reads on the developer's board. Separate from the customer's
// vocabulary for the request: the same piece of work, described to two people
// who need different things from the description.
public final class WorkflowStatusPresentation {

    private WorkflowStatusPresentation() {
    }

    public static StatusBadge badge(WorkflowStatus status) {
        return new StatusBadge(label(status), tone(status));
    }

    public static String label(WorkflowStatus status) {
        return switch (status) {
            case BACKLOG -> "Backlog";
            case IN_PROGRESS -> "In Progress";
            case TESTING -> "Testing";
            case DONE -> "Done";
        };
    }

    // The button says what the move means, not what the target is called:
    // "Test Failed" reads as a decision where "In Progress" reads as a
    // destination.
    public static String actionLabel(WorkflowStatus from, WorkflowStatus to) {
        if (from == WorkflowStatus.TESTING && to == WorkflowStatus.IN_PROGRESS) {
            return "Test Failed";
        }
        return switch (to) {
            case IN_PROGRESS -> "Start Work";
            case TESTING -> "Ready for Testing";
            case DONE -> "Mark as Done";
            case BACKLOG -> "Return to Backlog";
        };
    }

    private static StatusBadge.Tone tone(WorkflowStatus status) {
        return switch (status) {
            case BACKLOG -> StatusBadge.Tone.NEUTRAL;
            case IN_PROGRESS -> StatusBadge.Tone.ACTIVE;
            case TESTING -> StatusBadge.Tone.WARNING;
            case DONE -> StatusBadge.Tone.POSITIVE;
        };
    }
}