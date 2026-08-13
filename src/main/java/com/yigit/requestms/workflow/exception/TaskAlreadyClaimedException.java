package com.yigit.requestms.workflow.exception;

import com.yigit.requestms.common.exception.BaseException;

public class TaskAlreadyClaimedException extends BaseException {

    public TaskAlreadyClaimedException(Long taskId) {
        super("TASK_ALREADY_CLAIMED",
                "Task " + taskId + " was taken by someone else");
    }
}