package com.opsbrain.contextcollector.model;

/** Normalized severity. Each source's native levels are mapped onto this. */
public enum Severity {
    CRITICAL,   // page immediately
    HIGH,
    WARNING,
    INFO;

    /** Lenient parse — sources use "critical", "P1", "error", etc. */
    public static Severity fromRaw(String raw) {
        if (raw == null) return INFO;
        return switch (raw.trim().toLowerCase()) {
            case "critical", "crit", "p1", "sev1", "fatal", "emergency" -> CRITICAL;
            case "high", "error", "p2", "sev2", "major"                 -> HIGH;
            case "warning", "warn", "p3", "sev3", "minor"               -> WARNING;
            default                                                      -> INFO;
        };
    }
}