package com.opsbrain.alertingestion.mapper;

import com.opsbrain.alertingestion.dto.GrafanaWebhook;
import com.opsbrain.alertingestion.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GrafanaAlertMapper {

    private final FingerprintGenerator fingerprintGenerator;

    public List<NormalizedAlert> map(GrafanaWebhook webhook) {
        List<NormalizedAlert> result = new ArrayList<>();
        if (webhook.getAlerts() == null) return result;

        for (GrafanaWebhook.GrafanaAlert a : webhook.getAlerts()) {
            Map<String, String> labels = a.getLabels() == null ? Map.of() : a.getLabels();
            Map<String, String> ann    = a.getAnnotations() == null ? Map.of() : a.getAnnotations();

            String alertName = labels.getOrDefault("alertname", webhook.getTitle());
            String service   = labels.get("service");

            result.add(NormalizedAlert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .fingerprint(fingerprintGenerator.generate(
                            "GRAFANA", alertName, service, labels))
                    .source(AlertSource.GRAFANA)
                    .alertName(alertName)
                    .severity(Severity.fromRaw(labels.get("severity")))
                    .status(AlertStatus.fromRaw(a.getStatus()))
                    .summary(ann.getOrDefault("summary", webhook.getMessage()))
                    .description(ann.get("description"))
                    .service(service)
                    .namespace(labels.get("namespace"))
                    .cluster(labels.get("cluster"))
                    .labels(labels)
                    .annotations(ann)
                    .generatorUrl(a.getPanelURL() != null ? a.getPanelURL() : a.getDashboardURL())
                    .ingestedAt(Instant.now())
                    .build());
        }
        return result;
    }
}