package com.opsbrain.contextcollector.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsbrain.contextcollector.model.DeploymentContext;
import com.opsbrain.contextcollector.model.NormalizedAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the ArgoCD Application for the service and answers the key question:
 * "did a deploy land just before this alert?" — the most common root cause.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentCollector {

    private final RestClient argocdRestClient;

    @Value("${opsbrain.collect.deploy-correlation-minutes:30}")
    private long correlationMinutes;

    public DeploymentContext collect(NormalizedAlert alert) {
        // ArgoCD app name convention == service name (override via label if needed)
        String app = alert.getLabels() != null
                ? alert.getLabels().getOrDefault("argocd_app", alert.getService())
                : alert.getService();
        if (app == null) return DeploymentContext.empty("unknown");

        JsonNode resp = argocdRestClient.get()
                .uri("/api/v1/applications/{name}", app)
                .retrieve()
                .body(JsonNode.class);

        if (resp == null) return DeploymentContext.empty(app);

        JsonNode status = resp.path("status");
        String currentRev = status.path("sync").path("revision").asText(null);

        List<DeploymentContext.Revision> history = new ArrayList<>();
        Instant lastDeployedAt = null;

        for (JsonNode h : status.path("history")) {
            Instant deployedAt = parse(h.path("deployedAt").asText(null));
            history.add(DeploymentContext.Revision.builder()
                    .revision(h.path("revision").asText(null))
                    .deployedAt(deployedAt)
                    .build());
            if (deployedAt != null && (lastDeployedAt == null || deployedAt.isAfter(lastDeployedAt))) {
                lastDeployedAt = deployedAt;
            }
        }

        boolean recent = false;
        Instant alertStart = alert.getStartsAt() != null ? alert.getStartsAt() : alert.getIngestedAt();
        if (lastDeployedAt != null && alertStart != null) {
            Duration gap = Duration.between(lastDeployedAt, alertStart).abs();
            recent = gap.toMinutes() <= correlationMinutes;
        }

        return DeploymentContext.builder()
                .applicationName(app)
                .currentRevision(currentRev)
                .lastDeployedAt(lastDeployedAt)
                .recentlyDeployed(recent)
                .history(history.size() > 5 ? history.subList(0, 5) : history)
                .build();
    }

    private Instant parse(String s) {
        try { return s == null ? null : Instant.parse(s); }
        catch (Exception e) { return null; }
    }
}