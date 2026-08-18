package com.yigit.requestms.auth.exception;

import com.yigit.requestms.common.exception.BaseException;

public class WeakPasswordException extends BaseException {

    public WeakPasswordException() {
        super("WEAK_PASSWORD",
                "A password needs at least 8 characters, including a letter and a digit");
    }
}