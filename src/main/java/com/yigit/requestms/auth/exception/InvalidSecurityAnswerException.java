package com.yigit.requestms.auth.exception;

import com.yigit.requestms.common.exception.BaseException;

// Says the answer was wrong and nothing about how many tries are left. A count
// helps whoever is guessing far more than whoever forgot, who will get it on
// the first or second attempt or not at all.
public class InvalidSecurityAnswerException extends BaseException {

    public InvalidSecurityAnswerException() {
        super("INVALID_SECURITY_ANSWER", "That answer does not match");
    }
}