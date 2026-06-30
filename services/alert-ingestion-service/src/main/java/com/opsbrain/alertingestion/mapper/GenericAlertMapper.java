package com.opsbrain.alertingestion.mapper;

import com.opsbrain.alertingestion.dto.GenericAlertRequest;
import com.opsbrain.alertingestion.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GenericAlertMapper {

    private final FingerprintGenerator fingerprintGenerator;

    public NormalizedAlert map(GenericAlertRequest req) {
        Map<String, String> labels = req.getLabels() == null ? Map.of() : req.getLabels();

        return NormalizedAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .fingerprint(fingerprintGenerator.generate(
                        "GENERIC", req.getAlertName(), req.getService(), labels))
                .source(AlertSource.GENERIC)
                .alertName(req.getAlertName())
                .severity(Severity.fromRaw(req.getSeverity()))
                .status(AlertStatus.fromRaw(req.getStatus()))
                .summary(req.getSummary() != null ? req.getSummary() : req.getAlertName())
                .description(req.getDescription())
                .service(req.getService())
                .namespace(req.getNamespace())
                .cluster(req.getCluster())
                .labels(labels)
                .annotations(req.getAnnotations())
                .generatorUrl(req.getGeneratorUrl())
                .ingestedAt(Instant.now())
                .build();
    }
}