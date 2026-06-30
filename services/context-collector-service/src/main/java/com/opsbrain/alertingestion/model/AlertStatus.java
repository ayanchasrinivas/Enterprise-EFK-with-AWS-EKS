package com.opsbrain.contextcollector.model;

/** Firing = problem active. Resolved = source says it cleared. */
public enum AlertStatus {
    FIRING,
    RESOLVED;

    public static AlertStatus fromRaw(String raw) {
        if (raw == null) return FIRING;
        return raw.trim().equalsIgnoreCase("resolved") ? RESOLVED : FIRING;
    }
}