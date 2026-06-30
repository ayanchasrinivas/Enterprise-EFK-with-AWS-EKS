package com.opsbrain.contextcollector.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Slice of logs pulled from Elasticsearch around the alert window. */
@Value
@Builder
public class LogContext {

    long totalMatches;                  // total docs in the window
    Map<String, Long> levelCounts;      // {"error": 142, "warn": 30, ...}
    List<LogEntry> recentErrors;        // top-N most recent error/warn lines

    @Value
    @Builder
    public static class LogEntry {
        Instant timestamp;
        String level;
        String pod;
        String message;
    }

    /** Empty placeholder used when ES is unreachable — keeps bundle non-null. */
    public static LogContext empty() {
        return LogContext.builder()
                .totalMatches(0)
                .levelCounts(Map.of())
                .recentErrors(List.of())
                .build();
    }
}