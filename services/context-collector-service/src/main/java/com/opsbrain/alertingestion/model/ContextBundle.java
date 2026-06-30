package com.opsbrain.contextcollector.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Everything the AI service needs to reason about one alert.
 * Built by fanning out to 5 sources; any source may be empty if it was
 * unreachable — collectionErrors records which ones failed.
 */
@Value
@Builder
public class ContextBundle {

    String bundleId;
    NormalizedAlert alert;          // the original trigger

    LogContext logs;
    MetricContext metrics;
    List<KubernetesEvent> events;
    DeploymentContext deployment;
    List<DashboardLink> dashboards;

    Instant collectedAt;
    List<String> collectionErrors;  // e.g. ["prometheus: connection refused"]
}