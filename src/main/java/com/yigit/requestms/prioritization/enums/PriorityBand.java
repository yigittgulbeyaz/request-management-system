package com.yigit.requestms.prioritization.enums;

// Derived from the score for display only, and never stored: a band is a way
// of reading the number, not a second fact about the request.
public enum PriorityBand {

    LOW("Low"),
    MEDIUM("Medium"),
    CRITICAL("Critical");

    private final String label;

    PriorityBand(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PriorityBand ofScore(int score) {
        if (score <= 6) {
            return LOW;
        }
        return score <= 15 ? MEDIUM : CRITICAL;
    }
}