package com.opsbrain.contextcollector.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * ArgoCD deploy history for the service. The most valuable signal:
 * recentlyDeployed = did a rollout land shortly before the alert fired?
 */
@Value
@Builder
public class DeploymentContext {

    String applicationName;
    String currentRevision;        // git SHA currently synced
    Instant lastDeployedAt;
    boolean recentlyDeployed;      // deploy within the correlation window of alert start
    List<Revision> history;

    @Value
    @Builder
    public static class Revision {
        String revision;           // git SHA
        Instant deployedAt;
    }

    public static DeploymentContext empty(String app) {
        return DeploymentContext.builder()
                .applicationName(app)
                .recentlyDeployed(false)
                .history(List.of())
                .build();
    }
}