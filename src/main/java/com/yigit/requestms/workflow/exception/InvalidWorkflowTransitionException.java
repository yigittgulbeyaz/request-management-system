package com.yigit.requestms.workflow.exception;

import com.yigit.requestms.common.exception.BaseException;
import com.yigit.requestms.workflow.enums.WorkflowStatus;

public class InvalidWorkflowTransitionException extends BaseException {

    public InvalidWorkflowTransitionException(WorkflowStatus from, WorkflowStatus to) {
        super("INVALID_WORKFLOW_TRANSITION",
                "A task cannot move from " + from + " to " + to);
    }
}