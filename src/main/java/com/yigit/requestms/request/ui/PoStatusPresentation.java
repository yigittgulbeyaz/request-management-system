package com.yigit.requestms.request.ui;

import com.yigit.requestms.common.ui.StatusBadge;
import com.yigit.requestms.prioritization.enums.PriorityBand;
import com.yigit.requestms.request.enums.RequestStatus;

// The product owner sees the raw status and the score, where the customer sees
// neither. Same request, two vocabularies, so the mappings stay separate.
public final class PoStatusPresentation {

    private PoStatusPresentation() {
    }

    public static StatusBadge statusBadge(RequestStatus status) {
        return new StatusBadge(status.name(), statusTone(status));
    }

    // A null score is not missing data: it means no one has scored the request
    // yet, which is the state the pool exists to resolve.
    public static StatusBadge scoreBadge(Integer score) {
        if (score == null) {
            return new StatusBadge("Not Assigned", StatusBadge.Tone.NEUTRAL);
        }
        PriorityBand band = PriorityBand.ofScore(score);
        return new StatusBadge(score + " (" + band.getLabel() + ")", scoreTone(band));
    }

    private static StatusBadge.Tone statusTone(RequestStatus status) {
        return switch (status) {
            case NEW -> StatusBadge.Tone.NEUTRAL;
            case PRIORITIZED, IN_WORKFLOW -> StatusBadge.Tone.ACTIVE;
            case CLOSED -> StatusBadge.Tone.POSITIVE;
            case REJECTED -> StatusBadge.Tone.NEGATIVE;
        };
    }

    // Low reads as green because there is nothing to worry about, not because
    // it is good news: the colour answers "how soon", not "how welcome".
    private static StatusBadge.Tone scoreTone(PriorityBand band) {
        return switch (band) {
            case LOW -> StatusBadge.Tone.POSITIVE;
            case MEDIUM -> StatusBadge.Tone.ACTIVE;
            case HIGH -> StatusBadge.Tone.WARNING;
            case CRITICAL -> StatusBadge.Tone.NEGATIVE;
        };
    }
}