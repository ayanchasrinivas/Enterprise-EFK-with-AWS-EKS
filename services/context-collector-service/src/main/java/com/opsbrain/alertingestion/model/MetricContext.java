package com.opsbrain.contextcollector.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/** Instant snapshot of key Prometheus metrics for the affected service. */
@Value
@Builder
public class MetricContext {

    /** metricName -> current value, e.g. {"cpu_cores": 1.8, "memory_bytes": 5.1e8} */
    Map<String, Double> values;

    public static MetricContext empty() {
        return MetricContext.builder().values(Map.of()).build();
    }
}