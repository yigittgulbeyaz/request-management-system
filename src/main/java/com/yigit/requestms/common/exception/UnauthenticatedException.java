package com.yigit.requestms.common.exception;

// Common rather than feature-scoped: every feature resolves the acting user
// through the same path, so the failure belongs to none of them in particular.
public class UnauthenticatedException extends BaseException {

    public UnauthenticatedException(String message) {
        super("UNAUTHENTICATED", message);
    }
}