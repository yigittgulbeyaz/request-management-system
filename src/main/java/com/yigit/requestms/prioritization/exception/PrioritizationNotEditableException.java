package com.yigit.requestms.prioritization.exception;

import com.yigit.requestms.common.exception.BaseException;
import com.yigit.requestms.request.enums.RequestStatus;

public class PrioritizationNotEditableException extends BaseException {

    public PrioritizationNotEditableException(RequestStatus status) {
        super("PRIORITIZATION_NOT_EDITABLE",
                "A request in " + status + " can no longer be scored");
    }
}