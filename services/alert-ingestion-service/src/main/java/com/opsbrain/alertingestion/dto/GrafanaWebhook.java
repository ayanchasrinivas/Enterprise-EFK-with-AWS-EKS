package com.opsbrain.alertingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** Grafana unified alerting webhook payload (subset). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaWebhook {

    private String status;          // "firing" | "resolved"
    private String title;
    private String message;
    private List<GrafanaAlert> alerts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrafanaAlert {
        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private String dashboardURL;
        private String panelURL;
        private String fingerprint;
    }
}