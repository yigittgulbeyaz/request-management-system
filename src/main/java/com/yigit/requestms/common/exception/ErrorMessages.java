package com.yigit.requestms.common.exception;

import java.util.Map;

// Maps an error code to what the user should read. Kept apart from the
// exceptions themselves so the wording can move to the message bundles later
// without touching the classes that throw.
public final class ErrorMessages {

    private static final Map<String, String> BY_CODE = Map.of(
            "REQUEST_NOT_FOUND", "That request could not be found.",
            "INVALID_REQUEST_TRANSITION", "This request can no longer move to that state.",
            "PRIORITIZATION_NOT_EDITABLE", "This request can no longer be scored.",
            "REJECTION_REASON_REQUIRED", "A reason is required before rejecting a request.",
            "UNAUTHENTICATED", "Your session has ended. Please sign in again.",
            "INVALID_WORKFLOW_TRANSITION", "This task cannot move to that stage.",
            "WORKFLOW_ALREADY_EXISTS", "This request is already in development."
    );

    private static final String FALLBACK = "Something went wrong. Please try again.";

    private ErrorMessages() {
    }

    public static String forCode(String errorCode) {
        return BY_CODE.getOrDefault(errorCode, FALLBACK);
    }
}