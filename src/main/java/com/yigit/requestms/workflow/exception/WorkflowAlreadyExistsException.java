package com.yigit.requestms.workflow.exception;

import com.yigit.requestms.common.exception.BaseException;

public class WorkflowAlreadyExistsException extends BaseException {

    public WorkflowAlreadyExistsException(Long requestId) {
        super("WORKFLOW_ALREADY_EXISTS",
                "Request " + requestId + " has already been converted");
    }
}