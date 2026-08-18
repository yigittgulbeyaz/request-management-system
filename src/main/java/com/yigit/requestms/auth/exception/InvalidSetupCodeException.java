package com.yigit.requestms.auth.exception;

import com.yigit.requestms.common.exception.BaseException;

// One message for a code that never existed, a code already used, and a code
// that has expired. Telling them apart would let someone with a guessed code
// learn whether they had guessed a real one.
public class InvalidSetupCodeException extends BaseException {

    public InvalidSetupCodeException() {
        super("INVALID_SETUP_CODE",
                "That setup code is not valid. Ask an administrator for a new one");
    }
}