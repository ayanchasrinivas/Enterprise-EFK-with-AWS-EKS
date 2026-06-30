package com.opsbrain.incident.model;

public enum Severity {
    CRITICAL("P1", 1),
    HIGH("P2", 2),
    MEDIUM("P3", 3),
    LOW("P4", 4),
    INFO("P5", 5);

    private final String label;
    private final int priority;

    Severity(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    public int getPriority() {
        return priority;
    }

    public static Severity fromLabel(String label) {
        for (Severity s : values()) {
            if (s.label.equals(label)) {
                return s;
            }
        }
        return INFO;
    }
}
