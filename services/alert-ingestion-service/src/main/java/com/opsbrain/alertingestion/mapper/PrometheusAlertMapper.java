package com.opsbrain.alertingestion.mapper;

import com.opsbrain.alertingestion.dto.PrometheusWebhook;
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
public class PrometheusAlertMapper {

    private final FingerprintGenerator fingerprintGenerator;

    /** One webhook can carry many alerts — map each into its own NormalizedAlert. */
    public List<NormalizedAlert> map(PrometheusWebhook webhook) {
        List<NormalizedAlert> result = new ArrayList<>();
        if (webhook.getAlerts() == null) return result;

        for (PrometheusWebhook.PrometheusAlert a : webhook.getAlerts()) {
            Map<String, String> labels = a.getLabels() == null ? Map.of() : a.getLabels();
            Map<String, String> ann    = a.getAnnotations() == null ? Map.of() : a.getAnnotations();

            String alertName = labels.getOrDefault("alertname", "UnknownAlert");
            String service   = labels.getOrDefault("service",
                               labels.getOrDefault("job", null));

            result.add(NormalizedAlert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .fingerprint(fingerprintGenerator.generate(
                            "PROMETHEUS", alertName, service, labels))
                    .source(AlertSource.PROMETHEUS)
                    .alertName(alertName)
                    .severity(Severity.fromRaw(labels.get("severity")))
                    .status(AlertStatus.fromRaw(a.getStatus()))
                    .summary(ann.getOrDefault("summary", alertName))
                    .description(ann.get("description"))
                    .service(service)
                    .namespace(labels.get("namespace"))
                    .cluster(labels.get("cluster"))
                    .startsAt(a.getStartsAt())
                    .endsAt(a.getEndsAt())
                    .labels(labels)
                    .annotations(ann)
                    .generatorUrl(a.getGeneratorURL())
                    .ingestedAt(Instant.now())
                    .build());
        }
        return result;
    }
}