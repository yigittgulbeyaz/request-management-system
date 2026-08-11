package com.yigit.requestms.request.exception;

import com.yigit.requestms.common.exception.BaseException;
import com.yigit.requestms.request.enums.RequestStatus;

public class InvalidRequestTransitionException extends BaseException {

    private final RequestStatus from;
    private final RequestStatus to;

    public InvalidRequestTransitionException(RequestStatus from, RequestStatus to) {
        super("INVALID_REQUEST_TRANSITION",
                "Request cannot move from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public RequestStatus getFrom() {
        return from;
    }

    public RequestStatus getTo() {
        return to;
    }
}