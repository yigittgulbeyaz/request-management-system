package com.yigit.requestms.prioritization.enums;

// The descriptive label is what keeps scoring consistent between product
// owners: "3" on its own is a matter of opinion, "processes with a workaround"
// is a question with an answer.
public enum ImpactLevel {

    COSMETIC(1, "Cosmetic - no functional effect"),
    MINOR(2, "Minor - small inconvenience"),
    MODERATE(3, "Moderate - business processes with a workaround"),
    MAJOR(4, "Major - business processes with no workaround"),
    CRITICAL(5, "Critical - core business operations blocked");

    private final int value;
    private final String label;

    ImpactLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static ImpactLevel ofValue(int value) {
        for (ImpactLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("No impact level for value " + value);
    }
}