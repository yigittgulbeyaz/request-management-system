package com.yigit.requestms.user.exception;

import com.yigit.requestms.common.exception.BaseException;

public class DuplicateEmailException extends BaseException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "An account already exists for " + email);
    }
}