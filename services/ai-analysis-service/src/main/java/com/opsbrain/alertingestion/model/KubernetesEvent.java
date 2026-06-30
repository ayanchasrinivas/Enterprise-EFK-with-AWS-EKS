package com.opsbrain.aianalysis.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/** A single K8s event (Warning/Normal) involving the affected workload. */
@Value
@Builder
public class KubernetesEvent {
    String type;        // Normal | Warning
    String reason;      // OOMKilling, FailedScheduling, BackOff, Unhealthy...
    String message;
    String involvedObject;
    int count;
    Instant lastSeen;
}