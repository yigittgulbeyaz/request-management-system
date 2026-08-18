package com.yigit.requestms.common.exception;

import java.util.Map;

// Maps an error code to what the user should read. Kept apart from the
// exceptions themselves so the wording can move to the message bundles later
// without touching the classes that throw.
//
// Map.ofEntries rather than Map.of: the latter stops at ten pairs, and this
// list only grows.
public final class ErrorMessages {

    private static final Map<String, String> BY_CODE = Map.ofEntries(
            Map.entry("REQUEST_NOT_FOUND", "That request could not be found."),
            Map.entry("INVALID_REQUEST_TRANSITION",
                    "This request can no longer move to that state."),
            Map.entry("REJECTION_REASON_REQUIRED",
                    "A reason is required before rejecting a request."),
            Map.entry("PRIORITIZATION_NOT_EDITABLE",
                    "This request can no longer be scored."),
            Map.entry("WORKFLOW_ALREADY_EXISTS", "This request is already in development."),
            Map.entry("INVALID_WORKFLOW_TRANSITION", "This task cannot move to that stage."),
            Map.entry("TASK_NOT_FOUND", "That task could not be found."),
            Map.entry("TASK_ALREADY_CLAIMED", "Someone else took this task first."),
            Map.entry("TASK_NOT_YOURS", "This task is assigned to another developer."),
            Map.entry("UNAUTHENTICATED", "Your session has ended. Please sign in again."),
            Map.entry("USER_NOT_FOUND", "That user could not be found."),
            Map.entry("DUPLICATE_EMAIL", "An account already uses that email address."),
            Map.entry("LAST_ADMIN_PROTECTED",
                    "The only remaining administrator cannot be demoted or deactivated."),
            Map.entry("INVALID_SETUP_CODE",
                    "That setup code is not valid. Ask an administrator for a new one."),
            Map.entry("WEAK_PASSWORD",
                    "A password needs at least 8 characters, including a letter and a digit.")
    );

    // What a user reads when something failed for a reason they cannot act on.
    // Saying more would either mean nothing to them or say too much to someone
    // probing for a way in.
    private static final String FALLBACK = "Something went wrong. Please try again.";

    private ErrorMessages() {
    }

    // Null-checked before the lookup: an immutable map rejects a null key with
    // an exception of its own, and the caller passing null is the handler
    // dealing with something that was not a BaseException — which is to say,
    // already dealing with a failure.
    public static String forCode(String errorCode) {
        if (errorCode == null) {
            return FALLBACK;
        }
        return BY_CODE.getOrDefault(errorCode, FALLBACK);
    }

    // Exposed for the test that walks every exception in the codebase and
    // checks it has something to say to the user.
    static boolean hasMessageFor(String errorCode) {
        return BY_CODE.containsKey(errorCode);
    }
}