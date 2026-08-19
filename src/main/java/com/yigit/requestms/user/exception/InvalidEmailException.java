package com.yigit.requestms.user.exception;

import com.yigit.requestms.common.exception.BaseException;

public class InvalidEmailException extends BaseException {

    public InvalidEmailException() {
        super("INVALID_EMAIL", "That does not look like an email address");
    }
}