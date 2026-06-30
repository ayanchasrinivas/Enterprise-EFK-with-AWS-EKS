package com.opsbrain.oncall.model;

public enum Severity {
    CRITICAL("P1"),
    HIGH("P2"),
    MEDIUM("P3"),
    LOW("P4");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
