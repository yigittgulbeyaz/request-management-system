package com.yigit.requestms.prioritization.dto;

import com.yigit.requestms.prioritization.enums.ImpactLevel;
import com.yigit.requestms.prioritization.enums.UrgencyLevel;

// Carries the two inputs and nothing else. The score is absent because the
// database derives it; sending one would invite the two to disagree.
public record PrioritizationFormDto(
        ImpactLevel impact,
        UrgencyLevel urgency
) {
}