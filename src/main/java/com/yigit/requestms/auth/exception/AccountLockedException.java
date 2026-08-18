package com.yigit.requestms.auth.exception;

import com.yigit.requestms.common.exception.BaseException;

// Reached after too many wrong answers. An administrator clears it, which is
// deliberate: the point of a limit is that getting past it takes someone else.
public class AccountLockedException extends BaseException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED",
                "Too many incorrect answers. Ask an administrator to unlock the account");
    }
}