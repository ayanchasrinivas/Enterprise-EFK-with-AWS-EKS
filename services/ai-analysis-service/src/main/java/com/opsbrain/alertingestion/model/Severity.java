package com.opsbrain.aianalysis.model;

public enum Severity {
    CRITICAL, HIGH, WARNING, INFO;

    public static Severity fromRaw(String raw) {
        if (raw == null) return INFO;
        return switch (raw.trim().toUpperCase()) {
            case "CRITICAL" -> CRITICAL;
            case "HIGH"     -> HIGH;
            case "WARNING"  -> WARNING;
            default          -> INFO;
        };
    }
}