package com.opsbrain.alertingestion.service;

import com.opsbrain.alertingestion.model.NormalizedAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPublisher {

    private final KafkaTemplate<String, NormalizedAlert> kafkaTemplate;

    @Value("${opsbrain.kafka.topic.raw-alerts}")
    private String rawAlertsTopic;

    public void publish(List<NormalizedAlert> alerts) {
        alerts.forEach(this::publish);
    }

    public void publish(NormalizedAlert alert) {
        // Key by fingerprint → all firings of the same problem land on the same
        // partition, preserving order for that incident.
        kafkaTemplate.send(rawAlertsTopic, alert.getFingerprint(), alert)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish alert {} ({}): {}",
                                alert.getAlertId(), alert.getAlertName(), ex.getMessage());
                    } else {
                        log.info("Published alert {} [{}] source={} severity={} status={}",
                                alert.getAlertId(), alert.getAlertName(),
                                alert.getSource(), alert.getSeverity(), alert.getStatus());
                    }
                });
    }
}