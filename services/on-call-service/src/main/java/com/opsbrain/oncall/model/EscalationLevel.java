package com.opsbrain.oncall.model;

public enum EscalationLevel {
    LEVEL_1(1, 5),      // First responder, 5 minutes to acknowledge
    LEVEL_2(2, 10),     // Secondary, 10 minutes
    LEVEL_3(3, 15),     // Tertiary, 15 minutes
    MANAGER(4, 20);     // Manager, 20 minutes

    private final int level;
    private final int escalationMinutes;

    EscalationLevel(int level, int escalationMinutes) {
        this.level = level;
        this.escalationMinutes = escalationMinutes;
    }

    public int getLevel() {
        return level;
    }

    public int getEscalationMinutes() {
        return escalationMinutes;
    }
}
