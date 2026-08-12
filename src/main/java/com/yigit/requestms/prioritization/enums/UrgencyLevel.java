package com.yigit.requestms.prioritization.enums;

public enum UrgencyLevel {

    BACKLOG(1, "Backlog - no defined timeline"),
    LONG_TERM(2, "Long term - a future release"),
    MEDIUM_TERM(3, "Medium term - next sprint"),
    SHORT_TERM(4, "Short term - must land in the active sprint"),
    IMMEDIATE(5, "Immediate - requires intervention today");

    private final int value;
    private final String label;

    UrgencyLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static UrgencyLevel ofValue(int value) {
        for (UrgencyLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("No urgency level for value " + value);
    }
}