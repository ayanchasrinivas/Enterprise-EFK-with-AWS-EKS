package com.opsbrain.contextcollector.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsbrain.contextcollector.model.MetricContext;
import com.opsbrain.contextcollector.model.NormalizedAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs a handful of PromQL instant queries for the affected service and
 * captures the current scalar value of each. Templates use {ns}/{svc}
 * placeholders filled from the alert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollector {

    private final RestClient prometheusRestClient;

    /** metricName -> PromQL template. Kept small and high-signal. */
    private static final Map<String, String> QUERIES = Map.of(
            "cpu_cores",
            "sum(rate(container_cpu_usage_seconds_total{namespace=\"{ns}\",pod=~\"{svc}.*\"}[5m]))",
            "memory_bytes",
            "sum(container_memory_working_set_bytes{namespace=\"{ns}\",pod=~\"{svc}.*\"})",
            "restarts_total",
            "sum(kube_pod_container_status_restarts_total{namespace=\"{ns}\",pod=~\"{svc}.*\"})",
            "http_5xx_rate",
            "sum(rate(http_server_requests_seconds_count{namespace=\"{ns}\",status=~\"5..\"}[5m]))",
            "pods_not_ready",
            "sum(kube_pod_status_ready{namespace=\"{ns}\",condition=\"false\"})"
    );

    public MetricContext collect(NormalizedAlert alert) {
        if (alert.getNamespace() == null || alert.getService() == null) {
            return MetricContext.empty();
        }
        Map<String, Double> values = new LinkedHashMap<>();
        QUERIES.forEach((name, template) -> {
            String promql = template
                    .replace("{ns}", alert.getNamespace())
                    .replace("{svc}", alert.getService());
            Double v = queryScalar(promql);
            if (v != null) values.put(name, v);
        });
        return MetricContext.builder().values(values).build();
    }

    private Double queryScalar(String promql) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/v1/query")
                    .queryParam("query", promql).build().toUriString();

            JsonNode resp = prometheusRestClient.get().uri(uri)
                    .retrieve().body(JsonNode.class);

            JsonNode result = resp.path("data").path("result");
            if (result.isArray() && result.size() > 0) {
                // instant vector: value = [ <ts>, "<stringValue>" ]
                return result.get(0).path("value").get(1).asDouble();
            }
        } catch (Exception e) {
            log.debug("Prometheus query failed [{}]: {}", promql, e.getMessage());
        }
        return null;
    }
}