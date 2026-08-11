package com.yigit.requestms.request.ui;

import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.request.enums.RequestStatus;

// How a request status is presented to the customer who raised it. Kept apart
// from the views so the wording lives in one place: the same mapping is used by
// the list, the detail dialog and later by notification messages.
public final class CustomerStatusPresentation {

    private CustomerStatusPresentation() {
    }

    public static StatusBadge badge(RequestStatus status) {
        return new StatusBadge(label(status), tone(status));
    }

    public static String label(RequestStatus status) {
        return switch (status) {
            case NEW -> "Received";
            case PRIORITIZED -> "Under evaluation";
            case IN_WORKFLOW -> "In progress";
            case CLOSED -> "Completed";
            case REJECTED -> "Not taken forward";
        };
    }

    private static StatusBadge.Tone tone(RequestStatus status) {
        return switch (status) {
            case NEW -> StatusBadge.Tone.NEUTRAL;
            case PRIORITIZED, IN_WORKFLOW -> StatusBadge.Tone.ACTIVE;
            case CLOSED -> StatusBadge.Tone.POSITIVE;
            case REJECTED -> StatusBadge.Tone.NEGATIVE;
        };
    }
}