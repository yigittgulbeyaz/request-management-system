package com.yigit.requestms.common.exception;

// Extends RuntimeException on purpose: Spring does not roll back a transaction
// for checked exceptions, so a checked hierarchy could leave half-applied work
// committed.
public abstract class BaseException extends RuntimeException {

    private final String errorCode;

    protected BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // Stable identifier for the failure, independent of the message text.
    // The handler maps it to an HTTP status and the UI to a translated message.
    public String getErrorCode() {
        return errorCode;
    }
}