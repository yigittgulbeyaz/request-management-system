package com.yigit.requestms.prioritization.dto;

import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;

// What the scoring screen needs to render: the request being judged, and the
// existing score if there is one.
public record PrioritizationDetailDto(
        Long requestId,
        String requestTitle,
        String customerName,
        String description,
        ImpactLevel impact,
        UrgencyLevel urgency,
        Integer priorityScore
) {
    public boolean isScored() {
        return impact != null && urgency != null;
    }
}