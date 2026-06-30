package com.opsbrain.notification.model;

public enum NotificationChannel {
    SLACK("slack"),
    TEAMS("teams"),
    EMAIL("email");

    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationChannel fromValue(String value) {
        for (NotificationChannel channel : values()) {
            if (channel.value.equals(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown channel: " + value);
    }
}
