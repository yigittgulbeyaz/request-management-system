package com.yigit.requestms.request.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum RequestStatus {

    NEW,
    PRIORITIZED,
    IN_WORKFLOW,
    CLOSED,
    REJECTED;

    // A method rather than a field: enum constants cannot reference their
    // siblings during construction.
    public Set<RequestStatus> allowedTransitions() {
        return switch (this) {
            case NEW         -> EnumSet.of(PRIORITIZED, REJECTED);
            case PRIORITIZED -> EnumSet.of(IN_WORKFLOW, REJECTED);
            case IN_WORKFLOW -> EnumSet.of(CLOSED);
            case CLOSED, REJECTED -> Collections.emptySet();
        };
    }

    public boolean canTransitionTo(RequestStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    public boolean isFinal() {
        return allowedTransitions().isEmpty();
    }
}