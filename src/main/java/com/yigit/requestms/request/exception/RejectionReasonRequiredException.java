package com.yigit.requestms.request.exception;

import com.yigit.requestms.common.exception.BaseException;

public class RejectionReasonRequiredException extends BaseException {

    public RejectionReasonRequiredException() {
        super("REJECTION_REASON_REQUIRED",
                "A rejection reason is required and is shown to the customer");
    }
}