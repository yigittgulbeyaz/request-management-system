package com.yigit.requestms.prioritization.enums;

// Derived from the score for display only, and never stored: a band is a way
// of reading the number, not a second fact about the request.
public enum PriorityBand {

    LOW("Low", 20),
    MEDIUM("Medium", 10),
    HIGH("High", 5),
    CRITICAL("Critical", 2);

    private final String label;
    private final int allowedDays;

    PriorityBand(String label, int allowedDays) {
        this.label = label;
        this.allowedDays = allowedDays;
    }

    public String getLabel() {
        return label;
    }

    // How long the work gets once it is scheduled. The band already answers
    // "how urgent is this", so the answer to "by when" belongs beside it rather
    // than in a table somewhere else that could disagree.
    public int getAllowedDays() {
        return allowedDays;
    }

    public static PriorityBand ofScore(int score) {
        if (score <= 6) {
            return LOW;
        }
        if (score <= 12) {
            return MEDIUM;
        }
        return score <= 19 ? HIGH : CRITICAL;
    }
}