package com.yigit.requestms.workflow.exception;

import com.yigit.requestms.common.exception.BaseException;

public class TaskNotAssignedToYouException extends BaseException {

    public TaskNotAssignedToYouException(Long taskId) {
        super("TASK_NOT_YOURS",
                "Task " + taskId + " is assigned to another developer");
    }
}