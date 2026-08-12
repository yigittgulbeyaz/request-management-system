package com.yigit.requestms.workflow.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum WorkflowStatus {

    BACKLOG,
    IN_PROGRESS,
    TESTING,
    DONE;

    // A method rather than a field: enum constants cannot reference their
    // siblings during construction.
    public Set<WorkflowStatus> allowedTransitions() {
        return switch (this) {
            case BACKLOG -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(TESTING);
            // Failing a test sends the task back rather than forward, which is
            // the one reverse move the board allows.
            case TESTING -> EnumSet.of(DONE, IN_PROGRESS);
            case DONE -> Collections.emptySet();
        };
    }

    public boolean canTransitionTo(WorkflowStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    public boolean isFinal() {
        return allowedTransitions().isEmpty();
    }
}