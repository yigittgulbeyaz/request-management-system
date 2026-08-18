package com.yigit.requestms.user.exception;

import com.yigit.requestms.common.exception.BaseException;

public class IncorrectPasswordException extends BaseException {

    public IncorrectPasswordException() {
        super("INCORRECT_PASSWORD", "The current password is not correct");
    }
}