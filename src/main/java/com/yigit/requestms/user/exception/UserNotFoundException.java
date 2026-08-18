package com.yigit.requestms.user.exception;

import com.yigit.requestms.common.exception.BaseException;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND", "User not found: " + userId);
    }
}