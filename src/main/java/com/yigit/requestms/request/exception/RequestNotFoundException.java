package com.yigit.requestms.request.exception;

import com.yigit.requestms.common.exception.BaseException;

public class RequestNotFoundException extends BaseException {

    public RequestNotFoundException(Long requestId) {
        super("REQUEST_NOT_FOUND", "Request not found: " + requestId);
    }
}