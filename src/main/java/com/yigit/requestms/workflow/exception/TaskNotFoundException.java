package com.yigit.requestms.workflow.exception;

import com.yigit.requestms.common.exception.BaseException;

public class TaskNotFoundException extends BaseException {

    public TaskNotFoundException(Long taskId) {
        super("TASK_NOT_FOUND", "Task not found: " + taskId);
    }
}