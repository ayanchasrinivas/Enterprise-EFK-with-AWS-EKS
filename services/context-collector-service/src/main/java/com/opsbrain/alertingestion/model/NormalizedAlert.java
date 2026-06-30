package com.opsbrain.contextcollector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * The canonical alert event published to Kafka topic "raw-alerts".
 *
 * Every webhook format (Prometheus, Grafana, CloudWatch, K8s) is mapped into
 * THIS shape so downstream services never need to know the original source format.
 *
 * Immutable (@Value) — events should never be mutated after creation.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NormalizedAlert {

    /** Our own UUID for this alert event. */
    String alertId;

    /**
     * Stable dedup key derived from source + alertName + service + labels.
     * Two firings of the same problem share a fingerprint → lets incident-service
     * group them into one incident instead of creating duplicates.
     */
    String fingerprint;

    AlertSource source;
    String alertName;
    Severity severity;
    AlertStatus status;

    String summary;       // short human title
    String description;   // longer detail

    /** Affected service — used by context-collector to know what to query. */
    String service;
    String namespace;     // k8s namespace, if applicable
    String cluster;

    Instant startsAt;
    Instant endsAt;

    /** All original labels/annotations preserved for the LLM context. */
    Map<String, String> labels;
    Map<String, String> annotations;

    String generatorUrl;  // link back to source (Prometheus/Grafana panel)

    /** When OpsBrain received it. */
    Instant ingestedAt;
}