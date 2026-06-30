package com.opsbrain.alertingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Shape of the POST body AlertManager sends.
 * @see https://prometheus.io/docs/alerting/latest/configuration/#webhook_config
 * We ignore unknown fields so AlertManager version changes don't break us.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusWebhook {

    private String status;          // "firing" | "resolved" (group-level)
    private String externalURL;
    private List<PrometheusAlert> alerts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrometheusAlert {
        private String status;                 // per-alert status
        private Map<String, String> labels;    // alertname, severity, namespace, service...
        private Map<String, String> annotations; // summary, description
        private Instant startsAt;
        private Instant endsAt;
        private String generatorURL;
        private String fingerprint;            // AlertManager's own fingerprint
    }
}