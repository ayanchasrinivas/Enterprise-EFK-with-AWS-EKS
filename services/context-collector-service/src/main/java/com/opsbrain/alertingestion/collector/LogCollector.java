package com.opsbrain.contextcollector.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsbrain.contextcollector.model.LogContext;
import com.opsbrain.contextcollector.model.NormalizedAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

/**
 * Pulls error/warn logs for the affected service from Elasticsearch in the
 * window [alertStart - lookback, now], plus a per-level count aggregation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCollector {

    private final RestClient esRestClient;

    @Value("${opsbrain.collect.log-index-pattern:logs-*}")
    private String indexPattern;
    @Value("${opsbrain.collect.log-lookback-minutes:15}")
    private long lookbackMinutes;
    @Value("${opsbrain.collect.log-max-hits:20}")
    private int maxHits;

    public LogContext collect(NormalizedAlert alert) {
        Instant start = (alert.getStartsAt() != null ? alert.getStartsAt() : Instant.now())
                .minusSeconds(lookbackMinutes * 60);

        Map<String, Object> query = buildQuery(alert, start);

        JsonNode resp = esRestClient.post()
                .uri("/{index}/_search", indexPattern)
                .body(query)
                .retrieve()
                .body(JsonNode.class);

        if (resp == null) return LogContext.empty();

        long total = resp.path("hits").path("total").path("value").asLong(0);

        // Level aggregation
        Map<String, Long> levelCounts = new LinkedHashMap<>();
        for (JsonNode bucket : resp.path("aggregations").path("levels").path("buckets")) {
            levelCounts.put(bucket.path("key").asText(), bucket.path("doc_count").asLong());
        }

        // Recent error lines
        List<LogContext.LogEntry> recent = new ArrayList<>();
        for (JsonNode hit : resp.path("hits").path("hits")) {
            JsonNode src = hit.path("_source");
            recent.add(LogContext.LogEntry.builder()
                    .timestamp(parseTs(src.path("@timestamp").asText(null)))
                    .level(firstNonBlank(src.path("level").asText(null),
                                         src.path("log").path("level").asText(null), "unknown"))
                    .pod(src.path("kubernetes").path("pod_name").asText(null))
                    .message(firstNonBlank(src.path("message").asText(null),
                                           src.path("log").asText(null), ""))
                    .build());
        }

        return LogContext.builder()
                .totalMatches(total)
                .levelCounts(levelCounts)
                .recentErrors(recent)
                .build();
    }

    /** bool filter on service/namespace + time range, prefer error/warn, agg by level. */
    private Map<String, Object> buildQuery(NormalizedAlert alert, Instant start) {
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("range", Map.of("@timestamp",
                Map.of("gte", start.toString(), "lte", "now"))));

        if (alert.getNamespace() != null) {
            filters.add(Map.of("match", Map.of("kubernetes.namespace_name", alert.getNamespace())));
        }
        if (alert.getService() != null) {
            // match against common app-label fields
            filters.add(Map.of("multi_match", Map.of(
                    "query", alert.getService(),
                    "fields", List.of("kubernetes.labels.app",
                                      "kubernetes.container_name",
                                      "kubernetes.pod_name"))));
        }

        return Map.of(
                "size", maxHits,
                "sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))),
                "query", Map.of("bool", Map.of(
                        "filter", filters,
                        "should", List.of(
                                Map.of("match", Map.of("level", "error")),
                                Map.of("match", Map.of("level", "warn"))),
                        "minimum_should_match", 0)),
                "aggs", Map.of("levels",
                        Map.of("terms", Map.of("field", "level.keyword", "size", 10))));
    }

    private static Instant parseTs(String s) {
        try { return s == null ? null : Instant.parse(s); }
        catch (Exception e) { return null; }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
}